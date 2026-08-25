package com.sorsix.pawconnect.service

import com.sorsix.pawconnect.dto.request.CreatePetRequest
import com.sorsix.pawconnect.dto.request.PetPhotoRequest
import com.sorsix.pawconnect.dto.request.UpdatePetRequest
import com.sorsix.pawconnect.exception.ForbiddenOperationException
import com.sorsix.pawconnect.exception.ResourceNotFoundException
import com.sorsix.pawconnect.model.Pet
import com.sorsix.pawconnect.model.PetPhoto
import com.sorsix.pawconnect.model.User
import com.sorsix.pawconnect.repository.*
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
    fun createPet(request: CreatePetRequest): Pet {
        val species = petSpeciesRepository.findByCode(request.speciesCode)
            ?: throw ResourceNotFoundException("Species not found: ${request.speciesCode}")

        val breeds = if (request.breedCodes.isNotEmpty()) {
            val found = petBreedRepository.findByCodeIn(request.breedCodes)
            if (found.size != request.breedCodes.size) {
                val missing = request.breedCodes.filterNot { found.any { breed -> breed.code == it } }
                throw ResourceNotFoundException("Breeds not found: $missing")
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
        return petRepository.findByIdWithAllAssociations(saved.id!!)
            ?: throw IllegalStateException("Pet not found after save")
    }

    @Transactional(readOnly = true)
    fun getPetOrThrow(id: Long): Pet {
        return petRepository.findByIdWithAllAssociations(id) ?: throw ResourceNotFoundException("Pet not found: $id")
    }

    @Transactional
    fun updatePet(id: Long, request: UpdatePetRequest, currentUser: User): Pet {
        val pet = getPetOrThrow(id)
        ensureCanManagePet(pet, currentUser)

        request.speciesCode?.let { code ->
            pet.species =
                petSpeciesRepository.findByCode(code) ?: throw ResourceNotFoundException("Species not found: $code")
        }
        request.breedCodes?.let { codes ->
            if (codes.isNotEmpty()) {
                val found = petBreedRepository.findByCodeIn(codes)
                if (found.size != codes.size) {
                    val missing = codes.filterNot { code ->
                        found.any { breed -> breed.code == code }
                    }
                    throw ResourceNotFoundException("Breeds not found: $missing")
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
        return petRepository.findByIdWithAllAssociations(saved.id!!)
            ?: throw ResourceNotFoundException("Pet not found after update")
    }

    @Transactional
    fun addPhoto(petId: Long, request: PetPhotoRequest, currentUser: User): PetPhoto {
        val pet = getPetOrThrow(petId)
        ensureCanManagePet(pet, currentUser)

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
        return savedPhoto
    }

    fun uploadAndAddPhoto(
        petId: Long, file: MultipartFile, isPrimary: Boolean, displayOrder: Int, currentUser: User
    ): PetPhoto {
        val pet = getPetOrThrow(petId)
        ensureCanManagePet(pet, currentUser)

        val url = blobStorageService.upload(file, "pets/$petId")
        log.info("Photo uploaded to blob for pet {} by user {}", petId, currentUser.id)
        return addPhoto(
            petId, PetPhotoRequest(url = url, isPrimary = isPrimary, displayOrder = displayOrder), currentUser
        )
    }

    @Transactional
    fun removePhoto(petId: Long, photoId: Long, currentUser: User) {
        val pet = getPetOrThrow(petId)
        ensureCanManagePet(pet, currentUser)

        val photo = pet.photos.find { it.id == photoId } ?: throw ResourceNotFoundException("Photo not found: $photoId")
        pet.photos.remove(photo)
        petPhotoRepository.delete(photo)
        log.info("Photo {} removed from pet {} by user {}", photoId, petId, currentUser.id)
        runCatching { blobStorageService.delete(photo.url) }
            .onFailure { log.warn("Failed to delete blob for photo {} (pet {}): {}", photoId, petId, it.message) }
    }

    private fun ensureCanManagePet(pet: Pet, currentUser: User) {
        if (currentUser.isAdmin()) return
        val ownsListing = listingRepository.existsByPet_IdAndPostedBy_Id(pet.id!!, currentUser.id!!)
        if (!ownsListing) {
            throw ForbiddenOperationException("You do not own any listing for this pet")
        }
    }

    private fun normalizePrimaryPhoto(photos: MutableList<PetPhoto>) {
        val primaries = photos.filter { it.isPrimary }
        if (primaries.size > 1) {
            primaries.drop(1).forEach { it.isPrimary = false }
        } else if (primaries.isEmpty() && photos.isNotEmpty()) {
            photos.minByOrNull { it.displayOrder }?.isPrimary = true
        }
    }
}