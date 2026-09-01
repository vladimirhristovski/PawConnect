package com.sorsix.pawconnect.service

import com.sorsix.pawconnect.domain.Gender
import com.sorsix.pawconnect.domain.Pet
import com.sorsix.pawconnect.domain.PetBreed
import com.sorsix.pawconnect.domain.PetPhoto
import com.sorsix.pawconnect.domain.PetSpecies
import com.sorsix.pawconnect.domain.User
import com.sorsix.pawconnect.common.ListingStatusCodes
import com.sorsix.pawconnect.dto.request.CreatePetRequest
import com.sorsix.pawconnect.dto.request.PetPhotoRequest
import com.sorsix.pawconnect.dto.request.UpdatePetRequest
import com.sorsix.pawconnect.domain.result.AddPetPhotoResult
import com.sorsix.pawconnect.domain.result.CreatePetResult
import com.sorsix.pawconnect.domain.result.RemovePetPhotoResult
import com.sorsix.pawconnect.domain.result.UpdatePetResult
import com.sorsix.pawconnect.repository.ListingRepository
import com.sorsix.pawconnect.repository.PetBreedRepository
import com.sorsix.pawconnect.repository.PetPhotoRepository
import com.sorsix.pawconnect.repository.PetRepository
import com.sorsix.pawconnect.repository.PetSpeciesRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.multipart.MultipartFile
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PetServiceTest {

    private val petRepository = mockk<PetRepository>()
    private val petPhotoRepository = mockk<PetPhotoRepository>()
    private val petSpeciesRepository = mockk<PetSpeciesRepository>()
    private val petBreedRepository = mockk<PetBreedRepository>()
    private val listingRepository = mockk<ListingRepository>()
    private val blobStorageService = mockk<BlobStorageService>()
    private lateinit var service: PetService

    @BeforeEach
    fun setup() {
        service = PetService(
            petRepository, petPhotoRepository, petSpeciesRepository, petBreedRepository, listingRepository, blobStorageService
        )
    }

    private fun mockUser(id: Long = 1L, admin: Boolean = false): User {
        val user = mockk<User>(relaxed = true)
        every { user.id } returns id
        every { user.isAdmin() } returns admin
        return user
    }

    private fun mockSpecies(code: String = "DOG"): PetSpecies {
        val species = mockk<PetSpecies>(relaxed = true)
        every { species.code } returns code
        return species
    }

    private fun mockBreed(code: String): PetBreed {
        val breed = mockk<PetBreed>(relaxed = true)
        every { breed.code } returns code
        return breed
    }

    private fun mockPhoto(id: Long, url: String = "u$id", primary: Boolean = false, order: Int = 0): PetPhoto {
        val photo = mockk<PetPhoto>(relaxed = true)
        every { photo.id } returns id
        every { photo.url } returns url
        every { photo.isPrimary } returns primary
        every { photo.displayOrder } returns order
        return photo
    }

    private fun request(
        name: String = "Rex",
        speciesCode: String = "DOG",
        breedCodes: List<String> = emptyList(),
        photos: List<PetPhotoRequest> = emptyList()
    ) = CreatePetRequest(name = name, speciesCode = speciesCode, breedCodes = breedCodes, gender = Gender.MALE, photos = photos)

    @Test
    fun `createPet throws when species not found`() {
        every { petSpeciesRepository.findByCode("CAT") } returns null
        val result = service.createPet(request(speciesCode = "CAT"))
        assertIs<CreatePetResult.NotFound>(result)
        verify(exactly = 0) { petRepository.save(any()) }
    }

    @Test
    fun `createPet throws when a requested breed code does not exist`() {
        every { petSpeciesRepository.findByCode("DOG") } returns mockSpecies()
        every { petBreedRepository.findByCodeIn(listOf("A", "B")) } returns listOf(mockBreed("A"))
        val result = service.createPet(request(breedCodes = listOf("A", "B")))
        assertIs<CreatePetResult.NotFound>(result)
    }

    @Test
    fun `createPet persists the pet and reloads it with associations`() {
        val species = mockSpecies()
        val petSlot = mutableListOf<Pet>()
        every { petSpeciesRepository.findByCode("DOG") } returns species
        every { petRepository.save(capture(petSlot)) } answers { firstArg<Pet>().apply { id = 7L } }
        val reloaded = mockk<Pet>(relaxed = true)
        every { petRepository.findByIdWithAllAssociations(7L) } returns reloaded

        val result = service.createPet(request(name = "Bella"))

        val saved = petSlot.first()
        assertEquals("Bella", saved.name)
        assertEquals(species, saved.species)
        assertIs<CreatePetResult.Success>(result)
        assertEquals(reloaded, result.pet)
    }

    @Test
    fun `createPet builds photos with fallback display order and a primary when none is flagged`() {
        val petSlot = mutableListOf<Pet>()
        every { petSpeciesRepository.findByCode("DOG") } returns mockSpecies()
        every { petRepository.save(capture(petSlot)) } answers { firstArg<Pet>().apply { id = 9L } }
        every { petRepository.findByIdWithAllAssociations(9L) } answers { petSlot.last() }

        service.createPet(
            request(photos = listOf(PetPhotoRequest(url = "a"), PetPhotoRequest(url = "b")))
        )

        val photos = petSlot.last().photos
        assertEquals(2, photos.size)
        assertEquals(0, photos[0].displayOrder)
        assertEquals(1, photos[1].displayOrder)
        assertTrue(photos[0].isPrimary)
        assertFalse(photos[1].isPrimary)
    }

    @Test
    fun `findPet returns the pet when present`() {
        val pet = mockk<Pet>(relaxed = true)
        every { petRepository.findByIdWithAllAssociations(3L) } returns pet
        assertEquals(pet, service.findPet(3L))
    }

    @Test
    fun `findPet returns null when the pet is missing`() {
        every { petRepository.findByIdWithAllAssociations(3L) } returns null
        assertEquals(null, service.findPet(3L))
    }

    @Test
    fun `getVisiblePet returns null when the pet is missing`() {
        every { petRepository.findByIdWithAllAssociations(3L) } returns null
        assertEquals(null, service.getVisiblePet(3L, null))
    }

    @Test
    fun `getVisiblePet returns the pet to anyone when its listing is publicly visible`() {
        val pet = mockk<Pet>(relaxed = true)
        every { petRepository.findByIdWithAllAssociations(3L) } returns pet
        every { listingRepository.existsByPet_IdAndStatus_CodeInAndDeletedAtIsNull(3L, ListingStatusCodes.VISIBLE_PUBLIC) } returns true
        assertEquals(pet, service.getVisiblePet(3L, null))
    }

    @Test
    fun `getVisiblePet hides a pet whose only listing is a draft from an anonymous caller`() {
        val pet = mockk<Pet>(relaxed = true)
        every { petRepository.findByIdWithAllAssociations(3L) } returns pet
        every { listingRepository.existsByPet_IdAndStatus_CodeInAndDeletedAtIsNull(3L, ListingStatusCodes.VISIBLE_PUBLIC) } returns false
        assertEquals(null, service.getVisiblePet(3L, null))
    }

    @Test
    fun `getVisiblePet shows a draft pet to the listing owner`() {
        val pet = mockk<Pet>(relaxed = true)
        every { petRepository.findByIdWithAllAssociations(3L) } returns pet
        every { listingRepository.existsByPet_IdAndStatus_CodeInAndDeletedAtIsNull(3L, ListingStatusCodes.VISIBLE_PUBLIC) } returns false
        every { listingRepository.existsByPet_IdAndPostedBy_Id(3L, 1L) } returns true
        assertEquals(pet, service.getVisiblePet(3L, mockUser(id = 1L)))
    }

    @Test
    fun `getVisiblePet hides a draft pet from an unrelated authenticated user`() {
        val pet = mockk<Pet>(relaxed = true)
        every { petRepository.findByIdWithAllAssociations(3L) } returns pet
        every { listingRepository.existsByPet_IdAndStatus_CodeInAndDeletedAtIsNull(3L, ListingStatusCodes.VISIBLE_PUBLIC) } returns false
        every { listingRepository.existsByPet_IdAndPostedBy_Id(3L, 2L) } returns false
        assertEquals(null, service.getVisiblePet(3L, mockUser(id = 2L)))
    }

    @Test
    fun `getVisiblePet shows a draft pet to an admin regardless of ownership`() {
        val pet = mockk<Pet>(relaxed = true)
        every { petRepository.findByIdWithAllAssociations(3L) } returns pet
        assertEquals(pet, service.getVisiblePet(3L, mockUser(id = 99L, admin = true)))
    }

    @Test
    fun `updatePet throws when the pet is missing`() {
        every { petRepository.findByIdWithAllAssociations(1L) } returns null
        val result = service.updatePet(1L, UpdatePetRequest(name = "x"), mockUser())
        assertIs<UpdatePetResult.NotFound>(result)
    }

    @Test
    fun `updatePet forbids a user who owns no listing for the pet`() {
        val pet = mockk<Pet>(relaxed = true)
        every { pet.id } returns 5L
        every { petRepository.findByIdWithAllAssociations(5L) } returns pet
        every { listingRepository.existsByPet_IdAndPostedBy_Id(5L, 2L) } returns false
        val result = service.updatePet(5L, UpdatePetRequest(name = "x"), mockUser(id = 2L))
        assertIs<UpdatePetResult.Forbidden>(result)
    }

    @Test
    fun `updatePet only writes the fields present in the request`() {
        val pet = mockk<Pet>(relaxed = true)
        every { pet.id } returns 5L
        every { petRepository.findByIdWithAllAssociations(5L) } returns pet
        every { listingRepository.existsByPet_IdAndPostedBy_Id(5L, 1L) } returns true
        every { petRepository.save(any()) } returns pet

        service.updatePet(5L, UpdatePetRequest(name = "Milo", goodWithKids = true), mockUser(id = 1L))

        verify { pet.name = "Milo" }
        verify { pet.goodWithKids = true }
        verify(exactly = 0) { pet.description = any() }
        verify(exactly = 0) { pet.gender = any() }
    }

    @Test
    fun `updatePet lets an admin manage a pet they have no listing for`() {
        val pet = mockk<Pet>(relaxed = true)
        every { pet.id } returns 5L
        every { petRepository.findByIdWithAllAssociations(5L) } returns pet
        every { petRepository.save(any()) } returns pet

        service.updatePet(5L, UpdatePetRequest(name = "AdminName"), mockUser(id = 99L, admin = true))

        verify { pet.name = "AdminName" }
        verify(exactly = 0) { listingRepository.existsByPet_IdAndPostedBy_Id(any(), any()) }
    }

    @Test
    fun `updatePet clears breeds when given an empty breed list`() {
        val pet = mockk<Pet>(relaxed = true)
        every { pet.id } returns 5L
        every { petRepository.findByIdWithAllAssociations(5L) } returns pet
        every { listingRepository.existsByPet_IdAndPostedBy_Id(5L, 1L) } returns true
        every { petRepository.save(any()) } returns pet

        service.updatePet(5L, UpdatePetRequest(breedCodes = emptyList()), mockUser(id = 1L))

        verify { pet.breeds = mutableSetOf() }
    }

    @Test
    fun `updatePet throws when the new species code is unknown`() {
        val pet = mockk<Pet>(relaxed = true)
        every { pet.id } returns 5L
        every { petRepository.findByIdWithAllAssociations(5L) } returns pet
        every { listingRepository.existsByPet_IdAndPostedBy_Id(5L, 1L) } returns true
        every { petSpeciesRepository.findByCode("ZEBRA") } returns null

        val result = service.updatePet(5L, UpdatePetRequest(speciesCode = "ZEBRA"), mockUser(id = 1L))
        assertIs<UpdatePetResult.NotFound>(result)
    }

    @Test
    fun `addPhoto demotes existing primary photos when the new one is primary`() {
        val existing = mockPhoto(id = 1L, primary = true)
        every { existing.isPrimary = any() } returns Unit
        val pet = mockk<Pet>(relaxed = true)
        every { pet.id } returns 5L
        every { pet.photos } returns mutableListOf(existing)
        every { petRepository.findByIdWithAllAssociations(5L) } returns pet
        every { listingRepository.existsByPet_IdAndPostedBy_Id(5L, 1L) } returns true
        every { petPhotoRepository.save(any()) } answers { firstArg() }
        every { petRepository.save(any()) } returns pet

        service.addPhoto(5L, PetPhotoRequest(url = "new", isPrimary = true), mockUser(id = 1L))

        verify { existing.isPrimary = false }
        verify { petPhotoRepository.save(any()) }
    }

    @Test
    fun `addPhoto forbids a user with no listing for the pet`() {
        val pet = mockk<Pet>(relaxed = true)
        every { pet.id } returns 5L
        every { petRepository.findByIdWithAllAssociations(5L) } returns pet
        every { listingRepository.existsByPet_IdAndPostedBy_Id(5L, 2L) } returns false
        val result = service.addPhoto(5L, PetPhotoRequest(url = "x"), mockUser(id = 2L))
        assertIs<AddPetPhotoResult.Forbidden>(result)
    }

    @Test
    fun `uploadAndAddPhoto uploads to blob storage then attaches the returned url`() {
        val file = mockk<MultipartFile>(relaxed = true)
        val pet = mockk<Pet>(relaxed = true)
        every { pet.id } returns 5L
        every { pet.photos } returns mutableListOf()
        every { petRepository.findByIdWithAllAssociations(5L) } returns pet
        every { listingRepository.existsByPet_IdAndPostedBy_Id(5L, 1L) } returns true
        every { blobStorageService.upload(file, "pets/5") } returns "https://blob/pets/5/x.jpg"
        val photoSlot = slot<PetPhoto>()
        every { petPhotoRepository.save(capture(photoSlot)) } answers { photoSlot.captured }
        every { petRepository.save(any()) } returns pet

        service.uploadAndAddPhoto(5L, file, isPrimary = false, displayOrder = 2, currentUser = mockUser(id = 1L))

        verify { blobStorageService.upload(file, "pets/5") }
        assertEquals("https://blob/pets/5/x.jpg", photoSlot.captured.url)
    }

    @Test
    fun `uploadAndAddPhoto rejects an unauthorised user before touching blob storage`() {
        val file = mockk<MultipartFile>(relaxed = true)
        val pet = mockk<Pet>(relaxed = true)
        every { pet.id } returns 5L
        every { petRepository.findByIdWithAllAssociations(5L) } returns pet
        every { listingRepository.existsByPet_IdAndPostedBy_Id(5L, 2L) } returns false

        val result = service.uploadAndAddPhoto(5L, file, isPrimary = false, displayOrder = 0, currentUser = mockUser(id = 2L))
        assertIs<AddPetPhotoResult.Forbidden>(result)
        verify(exactly = 0) { blobStorageService.upload(any(), any()) }
    }

    @Test
    fun `removePhoto throws when the photo is not on the pet`() {
        val pet = mockk<Pet>(relaxed = true)
        every { pet.id } returns 5L
        every { pet.photos } returns mutableListOf(mockPhoto(id = 1L))
        every { petRepository.findByIdWithAllAssociations(5L) } returns pet
        every { listingRepository.existsByPet_IdAndPostedBy_Id(5L, 1L) } returns true

        val result = service.removePhoto(5L, 999L, mockUser(id = 1L))
        assertIs<RemovePetPhotoResult.NotFound>(result)
    }

    @Test
    fun `removePhoto deletes the photo and its blob`() {
        val photo = mockPhoto(id = 2L, url = "https://blob/p2")
        val pet = mockk<Pet>(relaxed = true)
        every { pet.id } returns 5L
        every { pet.photos } returns mutableListOf(photo)
        every { petRepository.findByIdWithAllAssociations(5L) } returns pet
        every { listingRepository.existsByPet_IdAndPostedBy_Id(5L, 1L) } returns true
        every { petPhotoRepository.delete(photo) } returns Unit
        every { blobStorageService.delete("https://blob/p2") } returns Unit

        service.removePhoto(5L, 2L, mockUser(id = 1L))

        verify { petPhotoRepository.delete(photo) }
        verify { blobStorageService.delete("https://blob/p2") }
    }

    @Test
    fun `removePhoto still succeeds when the blob delete fails`() {
        val photo = mockPhoto(id = 2L, url = "https://blob/p2")
        val pet = mockk<Pet>(relaxed = true)
        every { pet.id } returns 5L
        every { pet.photos } returns mutableListOf(photo)
        every { petRepository.findByIdWithAllAssociations(5L) } returns pet
        every { listingRepository.existsByPet_IdAndPostedBy_Id(5L, 1L) } returns true
        every { petPhotoRepository.delete(photo) } returns Unit
        every { blobStorageService.delete("https://blob/p2") } throws RuntimeException("blob down")

        service.removePhoto(5L, 2L, mockUser(id = 1L))

        verify { petPhotoRepository.delete(photo) }
    }
}
