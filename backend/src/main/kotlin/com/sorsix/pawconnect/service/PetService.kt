package com.sorsix.pawconnect.service

import com.sorsix.pawconnect.dto.request.CreatePetRequest
import com.sorsix.pawconnect.dto.request.PetPhotoRequest
import com.sorsix.pawconnect.dto.request.UpdatePetRequest
import com.sorsix.pawconnect.domain.Pet
import com.sorsix.pawconnect.domain.PetPhoto
import com.sorsix.pawconnect.domain.User
import com.sorsix.pawconnect.domain.result.AddPetPhotoResult
import com.sorsix.pawconnect.domain.result.CreatePetResult
import com.sorsix.pawconnect.domain.result.RemovePetPhotoResult
import com.sorsix.pawconnect.domain.result.UpdatePetResult
import com.sorsix.pawconnect.repository.*
import com.sorsix.pawconnect.common.ListingStatusCodes
import com.sorsix.pawconnect.common.denialReason
import com.sorsix.pawconnect.common.requireId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

@Service
class PetService(
    private val petRepository: PetRepository,
    private val petPhotoRepository: PetPhotoRepository,
    private val petSpeciesRepository: PetSpeciesRepository,
    private val petBreedRepository: PetBreedRepository,
    private val listingRepository: ListingRepository,
    private val blobStorageService: BlobStorageService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun createPet(request: CreatePetRequest): CreatePetResult {
        val species = petSpeciesRepository.findByCode(request.speciesCode)
            ?: return CreatePetResult.NotFound("Species not found: ${request.speciesCode}")

        val breeds = if (request.breedCodes.isNotEmpty()) {
            val found = petBreedRepository.findByCodeIn(request.breedCodes)
            if (found.size != request.breedCodes.size) {
                val missing = request.breedCodes.filterNot { found.any { breed -> breed.code == it } }
                return CreatePetResult.NotFound("Breeds not found: $missing")
            }
            found.toMutableSet()
        } else mutableSetOf()

        val pet = Pet(
            name = request.name, species = species, gender = request.gender
        ).apply {
            size = request.size
            age = request.age
            birthDate = request.birthDate
            weightKg = request.weightKg
            description = request.description
            goodWithKids = request.goodWithKids
            goodWithOtherPets = request.goodWithOtherPets
            this.breeds = breeds
        }

        val savedPet = petRepository.save(pet)

        request.photos.forEachIndexed { idx, photoReq ->
            val photo = PetPhoto(
                pet = savedPet,
                url = photoReq.url,
                isPrimary = photoReq.isPrimary,
                displayOrder = photoReq.displayOrder.takeIf { it > 0 } ?: idx)
            savedPet.photos.add(photo)
        }
        normalizePrimaryPhoto(savedPet.photos)
        val saved = petRepository.save(pet)
        log.info("Pet {} created ({} photo(s))", saved.id, saved.photos.size)
        val reloaded = petRepository.findByIdWithAllAssociations(saved.requireId())
            ?: throw IllegalStateException("Pet not found after save")
        return CreatePetResult.Success(reloaded)
    }

    @Transactional(readOnly = true)
    fun findPet(id: Long): Pet? = petRepository.findByIdWithAllAssociations(id)

    @Transactional(readOnly = true)
    fun getVisiblePet(id: Long, currentUser: User?): Pet? {
        val pet = petRepository.findByIdWithAllAssociations(id) ?: return null
        if (currentUser != null && currentUser.isAdmin()) return pet
        if (listingRepository.existsByPet_IdAndStatus_CodeInAndDeletedAtIsNull(id, ListingStatusCodes.VISIBLE_PUBLIC)) return pet
        if (currentUser != null && listingRepository.existsByPet_IdAndPostedBy_Id(id, currentUser.requireId())) return pet
        return null
    }

    @Transactional
    fun updatePet(id: Long, request: UpdatePetRequest, currentUser: User): UpdatePetResult {
        val pet = findPet(id) ?: return UpdatePetResult.NotFound("Pet not found: $id")
        canManagePetReason(pet, currentUser)?.let { return UpdatePetResult.Forbidden(it) }

        request.speciesCode?.let { code ->
            val species = petSpeciesRepository.findByCode(code)
                ?: return UpdatePetResult.NotFound("Species not found: $code")
            pet.species = species
        }
        request.breedCodes?.let { codes ->
            if (codes.isNotEmpty()) {
                val found = petBreedRepository.findByCodeIn(codes)
                if (found.size != codes.size) {
                    val missing = codes.filterNot { code ->
                        found.any { breed -> breed.code == code }
                    }
                    return UpdatePetResult.NotFound("Breeds not found: $missing")
                }
                pet.breeds = found.toMutableSet()
            } else {
                pet.breeds = mutableSetOf()
            }
        }
        request.name?.let { pet.name = it }
        request.gender?.let { pet.gender = it }
        request.size?.let { pet.size = it }
        request.age?.let { pet.age = it }
        request.birthDate?.let { pet.birthDate = it }
        request.weightKg?.let { pet.weightKg = it }
        request.description?.let { pet.description = it }
        request.goodWithKids?.let { pet.goodWithKids = it }
        request.goodWithOtherPets?.let { pet.goodWithOtherPets = it }

        val saved = petRepository.save(pet)
        log.info("Pet {} updated by user {}", saved.id, currentUser.id)
        val reloaded = petRepository.findByIdWithAllAssociations(saved.requireId())
            ?: return UpdatePetResult.NotFound("Pet not found after update")
        return UpdatePetResult.Success(reloaded)
    }

    @Transactional
    fun addPhoto(petId: Long, request: PetPhotoRequest, currentUser: User): AddPetPhotoResult {
        val pet = findPet(petId) ?: return AddPetPhotoResult.NotFound("Pet not found: $petId")
        canManagePetReason(pet, currentUser)?.let { return AddPetPhotoResult.Forbidden(it) }

        if (request.isPrimary) {
            pet.photos.filter { it.isPrimary }.forEach { it.isPrimary = false }
        }

        val photo = PetPhoto(
            pet = pet, url = request.url, isPrimary = request.isPrimary, displayOrder = request.displayOrder
        )
        pet.photos.add(photo)
        normalizePrimaryPhoto(pet.photos)

        val savedPhoto = petPhotoRepository.save(photo)
        petRepository.save(pet)
        log.info("Photo {} added to pet {} by user {}", savedPhoto.id, pet.id, currentUser.id)
        return AddPetPhotoResult.Success(savedPhoto)
    }

    fun uploadAndAddPhoto(
        petId: Long, file: MultipartFile, isPrimary: Boolean, displayOrder: Int, currentUser: User
    ): AddPetPhotoResult {
        val pet = findPet(petId) ?: return AddPetPhotoResult.NotFound("Pet not found: $petId")
        canManagePetReason(pet, currentUser)?.let { return AddPetPhotoResult.Forbidden(it) }

        val url = blobStorageService.upload(file, "pets/$petId")
        log.info("Photo uploaded to blob for pet {} by user {}", petId, currentUser.id)
        return addPhoto(
            petId, PetPhotoRequest(url = url, isPrimary = isPrimary, displayOrder = displayOrder), currentUser
        )
    }

    @Transactional
    fun removePhoto(petId: Long, photoId: Long, currentUser: User): RemovePetPhotoResult {
        val pet = findPet(petId) ?: return RemovePetPhotoResult.NotFound("Pet not found: $petId")
        canManagePetReason(pet, currentUser)?.let { return RemovePetPhotoResult.Forbidden(it) }

        val photo = pet.photos.find { it.id == photoId }
            ?: return RemovePetPhotoResult.NotFound("Photo not found: $photoId")
        pet.photos.remove(photo)
        petPhotoRepository.delete(photo)
        log.info("Photo {} removed from pet {} by user {}", photoId, petId, currentUser.id)
        runCatching { blobStorageService.delete(photo.url) }
            .onFailure { log.warn("Failed to delete blob for photo {} (pet {}): {}", photoId, petId, it.message) }
        return RemovePetPhotoResult.Success
    }

    private fun canManagePetReason(pet: Pet, currentUser: User): String? = denialReason(
        currentUser.isAdmin() || listingRepository.existsByPet_IdAndPostedBy_Id(pet.requireId(), currentUser.requireId()),
        "You do not own any listing for this pet"
    )

    private fun normalizePrimaryPhoto(photos: MutableList<PetPhoto>) {
        val primaries = photos.filter { it.isPrimary }
        if (primaries.size > 1) {
            primaries.drop(1).forEach { it.isPrimary = false }
        } else if (primaries.isEmpty() && photos.isNotEmpty()) {
            photos.minByOrNull { it.displayOrder }?.isPrimary = true
        }
    }
}
