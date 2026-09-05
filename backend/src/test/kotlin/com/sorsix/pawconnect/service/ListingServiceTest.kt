package com.sorsix.pawconnect.service

import com.sorsix.pawconnect.common.ApplicationStatusCodes
import com.sorsix.pawconnect.common.ListingStatusCodes
import com.sorsix.pawconnect.domain.AdoptionApplication
import com.sorsix.pawconnect.domain.ApplicationStatus
import com.sorsix.pawconnect.domain.Business
import com.sorsix.pawconnect.domain.Gender
import com.sorsix.pawconnect.domain.Listing
import com.sorsix.pawconnect.domain.ListingStatus
import com.sorsix.pawconnect.domain.Municipality
import com.sorsix.pawconnect.domain.Pet
import com.sorsix.pawconnect.domain.Size
import com.sorsix.pawconnect.domain.User
import com.sorsix.pawconnect.domain.result.CancelListingResult
import com.sorsix.pawconnect.domain.result.CreateListingResult
import com.sorsix.pawconnect.domain.result.CreatePetResult
import com.sorsix.pawconnect.domain.result.DeleteListingResult
import com.sorsix.pawconnect.domain.result.PublishListingResult
import com.sorsix.pawconnect.domain.result.UpdateListingResult
import com.sorsix.pawconnect.dto.request.CreateListingRequest
import com.sorsix.pawconnect.dto.request.CreatePetRequest
import com.sorsix.pawconnect.dto.request.UpdateListingRequest
import com.sorsix.pawconnect.repository.AdoptionApplicationRepository
import com.sorsix.pawconnect.repository.ApplicationStatusRepository
import com.sorsix.pawconnect.repository.BusinessRepository
import com.sorsix.pawconnect.repository.ListingRepository
import com.sorsix.pawconnect.repository.ListingStatusRepository
import com.sorsix.pawconnect.repository.MunicipalityRepository
import com.sorsix.pawconnect.repository.PetRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.hibernate.exception.ConstraintViolationException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.math.BigDecimal
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame

class ListingServiceTest {
    private val listingRepository = mockk<ListingRepository>()
    private val petService = mockk<PetService>()
    private val businessRepository = mockk<BusinessRepository>()
    private val municipalityRepository = mockk<MunicipalityRepository>()
    private val listingStatusRepository = mockk<ListingStatusRepository>()
    private val petRepository = mockk<PetRepository>()
    private val adoptionApplicationRepository = mockk<AdoptionApplicationRepository>()
    private val applicationStatusRepository = mockk<ApplicationStatusRepository>()
    private lateinit var service: ListingService

    @BeforeEach
    fun setup() {
        service =
            ListingService(
                listingRepository,
                petService,
                businessRepository,
                municipalityRepository,
                listingStatusRepository,
                petRepository,
                adoptionApplicationRepository,
                applicationStatusRepository,
            )
    }

    private fun mockUser(
        id: Long = 1L,
        admin: Boolean = false,
    ): User {
        val user = mockk<User>(relaxed = true)
        every { user.id } returns id
        every { user.isAdmin() } returns admin
        return user
    }

    private fun mockListingStatus(code: String): ListingStatus {
        val status = mockk<ListingStatus>(relaxed = true)
        every { status.code } returns code
        return status
    }

    private fun mockAppStatus(code: String): ApplicationStatus {
        val status = mockk<ApplicationStatus>(relaxed = true)
        every { status.code } returns code
        return status
    }

    private fun mockMunicipality(
        lat: BigDecimal? = BigDecimal("42.0"),
        lng: BigDecimal? = BigDecimal("21.4"),
    ): Municipality {
        val m = mockk<Municipality>(relaxed = true)
        every { m.latitude } returns lat
        every { m.longitude } returns lng
        return m
    }

    private fun mockListing(
        id: Long = 10L,
        ownerId: Long = 1L,
        statusCode: String = ListingStatusCodes.ACTIVE,
        deleted: Boolean = false,
    ): Listing {
        val listing = mockk<Listing>(relaxed = true)
        every { listing.id } returns id
        every { listing.postedBy } returns mockUser(id = ownerId)
        every { listing.status } returns mockListingStatus(statusCode)
        every { listing.deletedAt } returns if (deleted) java.time.Instant.now() else null
        return listing
    }

    private fun createRequest(
        petId: Long? = 5L,
        pet: CreatePetRequest? = null,
        businessId: Long? = null,
        municipalityCode: String = "SK-CENTAR",
        saveAsDraft: Boolean = false,
        latitude: BigDecimal? = null,
        longitude: BigDecimal? = null,
    ) = CreateListingRequest(
        petId = petId,
        pet = pet,
        businessId = businessId,
        municipalityCode = municipalityCode,
        title = "Adopt me",
        saveAsDraft = saveAsDraft,
        latitude = latitude,
        longitude = longitude,
    )

    @Test
    fun `createListing throws on the defensive fallback when neither petId nor pet is set`() {
        every { municipalityRepository.findByCode("SK-CENTAR") } returns mockMunicipality()
        assertFailsWith<IllegalStateException> {
            service.createListing(createRequest(petId = null, pet = null), mockUser())
        }
    }

    @Test
    fun `createListing throws when petId does not resolve`() {
        every { municipalityRepository.findByCode("SK-CENTAR") } returns mockMunicipality()
        every { petRepository.findById(5L) } returns Optional.empty()
        val result = service.createListing(createRequest(petId = 5L), mockUser())
        assertIs<CreateListingResult.NotFound>(result)
    }

    @Test
    fun `createListing throws conflict when the pet already has an open listing`() {
        val pet = mockk<Pet>(relaxed = true)
        every { pet.id } returns 5L
        every { municipalityRepository.findByCode("SK-CENTAR") } returns mockMunicipality()
        every { petRepository.findById(5L) } returns Optional.of(pet)
        every { pet.createdBy } returns mockUser(id = 1L)
        every {
            listingRepository.existsByPet_IdAndStatus_CodeInAndDeletedAtIsNull(5L, ListingStatusCodes.OPEN_STATUSES)
        } returns true
        val result = service.createListing(createRequest(petId = 5L), mockUser())
        assertIs<CreateListingResult.Conflict>(result)
    }

    @Test
    fun `createListing throws when the municipality code is unknown`() {
        every { municipalityRepository.findByCode("SK-CENTAR") } returns null
        val result = service.createListing(createRequest(petId = 5L), mockUser())
        assertIs<CreateListingResult.NotFound>(result)
    }

    @Test
    fun `createListing forbids attaching a business the user does not own`() {
        every { municipalityRepository.findByCode("SK-CENTAR") } returns mockMunicipality()
        val business = mockk<Business>(relaxed = true)
        every { business.owner } returns mockUser(id = 2L)
        every { businessRepository.findById(77L) } returns Optional.of(business)
        val result = service.createListing(createRequest(petId = 5L, businessId = 77L), mockUser(id = 1L))
        assertIs<CreateListingResult.Forbidden>(result)
    }

    @Test
    fun `createListing does not create a pet when the municipality is unknown`() {
        every { municipalityRepository.findByCode("SK-CENTAR") } returns null

        val result = service.createListing(createRequest(petId = null, pet = mockk(relaxed = true)), mockUser())

        assertIs<CreateListingResult.NotFound>(result)
        verify(exactly = 0) { petService.createPet(any(), any()) }
    }

    @Test
    fun `createListing translates a racing open-listing conflict into a clean error`() {
        val pet = mockk<Pet>(relaxed = true)
        every { pet.id } returns 5L
        every { petRepository.findById(5L) } returns Optional.of(pet)
        every { pet.createdBy } returns mockUser(id = 1L)
        every { listingRepository.existsByPet_IdAndStatus_CodeInAndDeletedAtIsNull(any(), any()) } returns false
        every { municipalityRepository.findByCode("SK-CENTAR") } returns mockMunicipality()
        every { listingStatusRepository.findByCode(ListingStatusCodes.ACTIVE) } returns mockListingStatus(ListingStatusCodes.ACTIVE)
        val constraintViolation =
            ConstraintViolationException(
                "duplicate key",
                java.sql.SQLException("duplicate key"),
                "uq_listings_pet_open",
            )
        every { listingRepository.save(any()) } throws DataIntegrityViolationException("insert failed", constraintViolation)

        val ex =
            assertFailsWith<IllegalArgumentException> {
                service.createListing(createRequest(petId = 5L), mockUser(id = 1L))
            }
        assertEquals("This pet already has an open listing", ex.message)
    }

    @Test
    fun `createListing uses the DRAFT status when saveAsDraft is set`() {
        val pet = mockk<Pet>(relaxed = true)
        every { pet.id } returns 5L
        every { petRepository.findById(5L) } returns Optional.of(pet)
        every { pet.createdBy } returns mockUser(id = 1L)
        every { listingRepository.existsByPet_IdAndStatus_CodeInAndDeletedAtIsNull(any(), any()) } returns false
        every { municipalityRepository.findByCode("SK-CENTAR") } returns mockMunicipality()
        val draft = mockListingStatus(ListingStatusCodes.DRAFT)
        every { listingStatusRepository.findByCode(ListingStatusCodes.DRAFT) } returns draft
        val listingSlot = mutableListOf<Listing>()
        every { listingRepository.save(capture(listingSlot)) } answers { firstArg<Listing>().apply { id = 30L } }
        val reloaded = mockListing(id = 30L)
        every { listingRepository.findByIdWithAllAssociations(30L) } returns reloaded

        val result = service.createListing(createRequest(petId = 5L, saveAsDraft = true), mockUser(id = 1L))

        assertSame(draft, listingSlot.first().status)
        assertIs<CreateListingResult.Success>(result)
        assertSame(reloaded, result.listing)
    }

    @Test
    fun `createListing creates an inline pet and defaults latitude longitude to the municipality`() {
        val inlinePet = mockk<Pet>(relaxed = true)
        every { inlinePet.id } returns 5L
        every { petService.createPet(any(), any()) } returns CreatePetResult.Success(inlinePet)
        every { listingRepository.existsByPet_IdAndStatus_CodeInAndDeletedAtIsNull(any(), any()) } returns false
        val municipality = mockMunicipality(lat = BigDecimal("41.1"), lng = BigDecimal("20.2"))
        every { municipalityRepository.findByCode("SK-CENTAR") } returns municipality
        every { listingStatusRepository.findByCode(ListingStatusCodes.ACTIVE) } returns mockListingStatus(ListingStatusCodes.ACTIVE)
        val listingSlot = mutableListOf<Listing>()
        every { listingRepository.save(capture(listingSlot)) } answers { firstArg<Listing>().apply { id = 31L } }
        every { listingRepository.findByIdWithAllAssociations(31L) } returns mockListing(id = 31L)

        service.createListing(
            createRequest(petId = null, pet = mockk(relaxed = true), latitude = null, longitude = null),
            mockUser(id = 1L),
        )

        verify { petService.createPet(any(), any()) }
        assertEquals(BigDecimal("41.1"), listingSlot.first().latitude)
        assertEquals(BigDecimal("20.2"), listingSlot.first().longitude)
    }

    @Test
    fun `createListing throws when the initial status row is missing`() {
        val pet = mockk<Pet>(relaxed = true)
        every { pet.id } returns 5L
        every { petRepository.findById(5L) } returns Optional.of(pet)
        every { pet.createdBy } returns mockUser(id = 1L)
        every { listingRepository.existsByPet_IdAndStatus_CodeInAndDeletedAtIsNull(any(), any()) } returns false
        every { municipalityRepository.findByCode("SK-CENTAR") } returns mockMunicipality()
        every { listingStatusRepository.findByCode(ListingStatusCodes.ACTIVE) } returns null
        assertFailsWith<IllegalStateException> {
            service.createListing(createRequest(petId = 5L), mockUser())
        }
    }

    @Test
    fun `createListing forbids attaching a pet the user does not own`() {
        val pet = mockk<Pet>(relaxed = true)
        every { pet.id } returns 5L
        every { municipalityRepository.findByCode("SK-CENTAR") } returns mockMunicipality()
        every { petRepository.findById(5L) } returns Optional.of(pet)
        every { pet.createdBy } returns mockUser(id = 999L)
        val result = service.createListing(createRequest(petId = 5L), mockUser(id = 1L))
        assertIs<CreateListingResult.Forbidden>(result)
    }

    @Test
    fun `getVisibleListing returns null when the listing does not exist`() {
        every { listingRepository.findByIdWithAllAssociations(1L) } returns null
        assertNull(service.getVisibleListing(1L, null))
    }

    @Test
    fun `getVisibleListing hides a soft-deleted listing`() {
        every { listingRepository.findByIdWithAllAssociations(1L) } returns mockListing(id = 1L, deleted = true)
        assertNull(service.getVisibleListing(1L, mockUser()))
    }

    @Test
    fun `getVisibleListing returns a publicly visible listing to anyone`() {
        val listing = mockListing(id = 1L, statusCode = ListingStatusCodes.ADOPTED)
        every { listingRepository.findByIdWithAllAssociations(1L) } returns listing
        assertSame(listing, service.getVisibleListing(1L, null))
    }

    @Test
    fun `getVisibleListing hides a DRAFT listing from anonymous users`() {
        every { listingRepository.findByIdWithAllAssociations(1L) } returns mockListing(id = 1L, statusCode = ListingStatusCodes.DRAFT)
        assertNull(service.getVisibleListing(1L, null))
    }

    @Test
    fun `getVisibleListing shows a DRAFT listing to its owner`() {
        val listing = mockListing(id = 1L, ownerId = 8L, statusCode = ListingStatusCodes.DRAFT)
        every { listingRepository.findByIdWithAllAssociations(1L) } returns listing
        assertSame(listing, service.getVisibleListing(1L, mockUser(id = 8L)))
    }

    @Test
    fun `getVisibleListing shows a DRAFT listing to an admin`() {
        val listing = mockListing(id = 1L, ownerId = 8L, statusCode = ListingStatusCodes.DRAFT)
        every { listingRepository.findByIdWithAllAssociations(1L) } returns listing
        assertSame(listing, service.getVisibleListing(1L, mockUser(id = 99L, admin = true)))
    }

    @Test
    fun `getVisibleListing hides a DRAFT listing from an unrelated user`() {
        every { listingRepository.findByIdWithAllAssociations(1L) } returns
            mockListing(id = 1L, ownerId = 8L, statusCode = ListingStatusCodes.DRAFT)
        assertNull(service.getVisibleListing(1L, mockUser(id = 3L)))
    }

    @Test
    fun `publishListing throws when the listing is missing`() {
        every { listingRepository.findByIdWithAllAssociations(1L) } returns null
        assertIs<PublishListingResult.NotFound>(service.publishListing(1L, mockUser()))
    }

    @Test
    fun `publishListing forbids a non-owner`() {
        every { listingRepository.findByIdWithAllAssociations(10L) } returns
            mockListing(id = 10L, ownerId = 1L, statusCode = ListingStatusCodes.DRAFT)
        assertIs<PublishListingResult.Forbidden>(service.publishListing(10L, mockUser(id = 2L)))
    }

    @Test
    fun `publishListing throws conflict when the listing is not in DRAFT`() {
        every { listingRepository.findByIdWithAllAssociations(10L) } returns
            mockListing(id = 10L, ownerId = 1L, statusCode = ListingStatusCodes.ACTIVE)
        assertIs<PublishListingResult.Conflict>(service.publishListing(10L, mockUser(id = 1L)))
    }

    @Test
    fun `publishListing moves a DRAFT listing to ACTIVE`() {
        val listing = mockListing(id = 10L, ownerId = 1L, statusCode = ListingStatusCodes.DRAFT)
        every { listingRepository.findByIdWithAllAssociations(10L) } returns listing
        val active = mockListingStatus(ListingStatusCodes.ACTIVE)
        every { listingStatusRepository.findByCode(ListingStatusCodes.ACTIVE) } returns active
        every { listingRepository.save(listing) } returns listing

        service.publishListing(10L, mockUser(id = 1L))

        verify { listing.status = active }
        verify { listingRepository.save(listing) }
    }

    @Test
    fun `updateListing throws conflict when the listing is adopted`() {
        every { listingRepository.findByIdWithAllAssociations(10L) } returns
            mockListing(id = 10L, ownerId = 1L, statusCode = ListingStatusCodes.ADOPTED)
        val result = service.updateListing(10L, UpdateListingRequest(title = "x"), mockUser(id = 1L))
        assertIs<UpdateListingResult.Conflict>(result)
    }

    @Test
    fun `updateListing writes only the provided fields`() {
        val listing = mockListing(id = 10L, ownerId = 1L, statusCode = ListingStatusCodes.ACTIVE)
        every { listingRepository.findByIdWithAllAssociations(10L) } returns listing
        every { listingRepository.save(listing) } returns listing

        service.updateListing(10L, UpdateListingRequest(title = "New title"), mockUser(id = 1L))

        verify { listing.title = "New title" }
        verify(exactly = 0) { listing.description = any() }
    }

    @Test
    fun `updateListing rejects an unknown municipality code`() {
        val listing = mockListing(id = 10L, ownerId = 1L, statusCode = ListingStatusCodes.DRAFT)
        every { listingRepository.findByIdWithAllAssociations(10L) } returns listing
        every { municipalityRepository.findByCode("BAD") } returns null
        val result = service.updateListing(10L, UpdateListingRequest(municipalityCode = "BAD"), mockUser(id = 1L))
        assertIs<UpdateListingResult.NotFound>(result)
    }

    @Test
    fun `updateListing applies explicit latitude and longitude when both are given`() {
        val listing = mockListing(id = 10L, ownerId = 1L, statusCode = ListingStatusCodes.ACTIVE)
        every { listingRepository.findByIdWithAllAssociations(10L) } returns listing
        every { listingRepository.save(listing) } returns listing

        service.updateListing(
            10L,
            UpdateListingRequest(latitude = BigDecimal("1.5"), longitude = BigDecimal("2.5")),
            mockUser(id = 1L),
        )

        verify { listing.latitude = BigDecimal("1.5") }
        verify { listing.longitude = BigDecimal("2.5") }
    }

    @Test
    fun `cancelListing throws conflict when already cancelled`() {
        every { listingRepository.findByIdWithAllAssociations(10L) } returns
            mockListing(id = 10L, ownerId = 1L, statusCode = ListingStatusCodes.CANCELLED)
        assertIs<CancelListingResult.Conflict>(service.cancelListing(10L, mockUser(id = 1L)))
    }

    @Test
    fun `cancelListing sets CANCELLED and rejects pending applications`() {
        val listing = mockListing(id = 10L, ownerId = 1L, statusCode = ListingStatusCodes.ACTIVE)
        every { listingRepository.findByIdWithAllAssociations(10L) } returns listing
        val cancelled = mockListingStatus(ListingStatusCodes.CANCELLED)
        every { listingStatusRepository.findByCode(ListingStatusCodes.CANCELLED) } returns cancelled
        val pendingApp = mockk<AdoptionApplication>(relaxed = true)
        every {
            adoptionApplicationRepository.findByListing_IdAndStatus_CodeInAndDeletedAtIsNull(10L, ApplicationStatusCodes.PENDING_STATUSES)
        } returns listOf(pendingApp)
        val rejected = mockAppStatus(ApplicationStatusCodes.REJECTED)
        every { applicationStatusRepository.findByCode(ApplicationStatusCodes.REJECTED) } returns rejected
        every { adoptionApplicationRepository.saveAll(any<List<AdoptionApplication>>()) } returns emptyList()
        every { listingRepository.save(listing) } returns listing

        service.cancelListing(10L, mockUser(id = 1L))

        verify { listing.status = cancelled }
        verify { pendingApp.status = rejected }
        verify { adoptionApplicationRepository.saveAll(listOf(pendingApp)) }
    }

    @Test
    fun `cancelListing skips application handling when there are no pending applications`() {
        val listing = mockListing(id = 10L, ownerId = 1L, statusCode = ListingStatusCodes.ACTIVE)
        every { listingRepository.findByIdWithAllAssociations(10L) } returns listing
        every { listingStatusRepository.findByCode(ListingStatusCodes.CANCELLED) } returns mockListingStatus(ListingStatusCodes.CANCELLED)
        every {
            adoptionApplicationRepository.findByListing_IdAndStatus_CodeInAndDeletedAtIsNull(10L, ApplicationStatusCodes.PENDING_STATUSES)
        } returns emptyList()
        every { listingRepository.save(listing) } returns listing

        service.cancelListing(10L, mockUser(id = 1L))

        verify(exactly = 0) { adoptionApplicationRepository.saveAll(any<List<AdoptionApplication>>()) }
    }

    @Test
    fun `rejectPendingApplications rejects across multiple listings with a null reviewer`() {
        val listingA = mockListing(id = 20L)
        val listingB = mockListing(id = 21L)
        val appA = mockk<AdoptionApplication>(relaxed = true)
        val appB = mockk<AdoptionApplication>(relaxed = true)
        every {
            adoptionApplicationRepository.findByListing_IdAndStatus_CodeInAndDeletedAtIsNull(20L, ApplicationStatusCodes.PENDING_STATUSES)
        } returns listOf(appA)
        every {
            adoptionApplicationRepository.findByListing_IdAndStatus_CodeInAndDeletedAtIsNull(21L, ApplicationStatusCodes.PENDING_STATUSES)
        } returns listOf(appB)
        val rejected = mockAppStatus(ApplicationStatusCodes.REJECTED)
        every { applicationStatusRepository.findByCode(ApplicationStatusCodes.REJECTED) } returns rejected
        every { adoptionApplicationRepository.saveAll(any<List<AdoptionApplication>>()) } returns listOf(appA, appB)

        val count = service.rejectPendingApplications(listOf(listingA, listingB), reviewedBy = null)

        assertEquals(2, count)
        verify { appA.status = rejected }
        verify { appB.status = rejected }
        verify { appA.reviewedBy = null }
        verify { appB.reviewedBy = null }
    }

    @Test
    fun `rejectPendingApplications returns zero without touching status lookup when nothing is pending`() {
        val listing = mockListing(id = 22L)
        every {
            adoptionApplicationRepository.findByListing_IdAndStatus_CodeInAndDeletedAtIsNull(22L, ApplicationStatusCodes.PENDING_STATUSES)
        } returns emptyList()

        val count = service.rejectPendingApplications(listOf(listing), reviewedBy = null)

        assertEquals(0, count)
        verify(exactly = 0) { applicationStatusRepository.findByCode(any()) }
    }

    @Test
    fun `deleteListing forbids a non-owner`() {
        every { listingRepository.findByIdWithAllAssociations(10L) } returns mockListing(id = 10L, ownerId = 1L)
        assertIs<DeleteListingResult.Forbidden>(service.deleteListing(10L, mockUser(id = 2L)))
    }

    @Test
    fun `deleteListing is a no-op when the listing is already soft-deleted`() {
        every { listingRepository.findByIdWithAllAssociations(10L) } returns mockListing(id = 10L, ownerId = 1L, deleted = true)
        service.deleteListing(10L, mockUser(id = 1L))
        verify(exactly = 0) { listingRepository.save(any()) }
    }

    @Test
    fun `deleteListing soft-deletes for the owner`() {
        val listing = mockListing(id = 10L, ownerId = 1L)
        every { listingRepository.findByIdWithAllAssociations(10L) } returns listing
        every { listingRepository.save(listing) } returns listing
        service.deleteListing(10L, mockUser(id = 1L))
        verify { listing.deletedAt = any() }
        verify { listingRepository.save(listing) }
    }

    @Test
    fun `markAdopted throws when the ADOPTED status row is missing`() {
        every { listingStatusRepository.findByCode(ListingStatusCodes.ADOPTED) } returns null
        assertFailsWith<IllegalStateException> { service.markAdopted(mockListing()) }
    }

    @Test
    fun `markAdopted sets the ADOPTED status and saves`() {
        val listing = mockListing(id = 10L)
        val adopted = mockListingStatus(ListingStatusCodes.ADOPTED)
        every { listingStatusRepository.findByCode(ListingStatusCodes.ADOPTED) } returns adopted
        every { listingRepository.save(listing) } returns listing
        service.markAdopted(listing)
        verify { listing.status = adopted }
        verify { listingRepository.save(listing) }
    }

    @Test
    fun `listMyListings delegates to the repository`() {
        val pageable = PageRequest.of(0, 20)
        every { listingRepository.findMyListingsWithAssociations(1L, pageable) } returns PageImpl(emptyList())
        service.listMyListings(mockUser(id = 1L), pageable)
        verify { listingRepository.findMyListingsWithAssociations(1L, pageable) }
    }

    @Test
    fun `adminSearchListings delegates with the status filter`() {
        val pageable = PageRequest.of(0, 20)
        every { listingRepository.findAllWithAssociations("ACTIVE", pageable) } returns PageImpl(emptyList())
        service.adminSearchListings("ACTIVE", pageable)
        verify { listingRepository.findAllWithAssociations("ACTIVE", pageable) }
    }

    @Test
    fun `searchNearby delegates to the repository native query with all filters`() {
        val pageable = PageRequest.of(0, 20)
        every {
            listingRepository.findNearby(
                42.0,
                21.4,
                5.0,
                "DOG",
                "SK-CENTAR",
                "SMALL",
                "MALE",
                true,
                false,
                BigDecimal("10"),
                BigDecimal("100"),
                pageable,
            )
        } returns PageImpl(emptyList())

        service.searchNearby(
            BigDecimal("42.0"),
            BigDecimal("21.4"),
            5.0,
            "DOG",
            "SK-CENTAR",
            Size.SMALL,
            Gender.MALE,
            true,
            false,
            BigDecimal("10"),
            BigDecimal("100"),
            pageable,
        )

        verify {
            listingRepository.findNearby(
                42.0,
                21.4,
                5.0,
                "DOG",
                "SK-CENTAR",
                "SMALL",
                "MALE",
                true,
                false,
                BigDecimal("10"),
                BigDecimal("100"),
                pageable,
            )
        }
    }
}
