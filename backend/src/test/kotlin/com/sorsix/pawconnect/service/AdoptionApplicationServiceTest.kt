package com.sorsix.pawconnect.service

import com.sorsix.pawconnect.common.ApplicationStatusCodes
import com.sorsix.pawconnect.common.ListingStatusCodes
import com.sorsix.pawconnect.domain.AdoptionApplication
import com.sorsix.pawconnect.domain.ApplicationStatus
import com.sorsix.pawconnect.domain.Listing
import com.sorsix.pawconnect.domain.ListingStatus
import com.sorsix.pawconnect.domain.User
import com.sorsix.pawconnect.domain.result.ListApplicationsForListingResult
import com.sorsix.pawconnect.domain.result.ReviewApplicationResult
import com.sorsix.pawconnect.domain.result.SubmitApplicationResult
import com.sorsix.pawconnect.domain.result.WithdrawApplicationResult
import com.sorsix.pawconnect.dto.request.ApplicationDecision
import com.sorsix.pawconnect.dto.request.CreateApplicationRequest
import com.sorsix.pawconnect.repository.AdoptionApplicationRepository
import com.sorsix.pawconnect.repository.ApplicationStatusRepository
import com.sorsix.pawconnect.repository.ListingRepository
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame

class AdoptionApplicationServiceTest {
    private val applicationRepository = mockk<AdoptionApplicationRepository>()
    private val listingRepository = mockk<ListingRepository>()
    private val applicationStatusRepository = mockk<ApplicationStatusRepository>()
    private val listingService = mockk<ListingService>()
    private lateinit var service: AdoptionApplicationService

    @BeforeEach
    fun setup() {
        service =
            AdoptionApplicationService(
                applicationRepository,
                listingRepository,
                applicationStatusRepository,
                listingService,
            )
    }

    private fun mockUser(
        id: Long = 1L,
        admin: Boolean = false,
        phone: String? = "070000000",
        email: String = "u$id@mail.test",
    ): User {
        val user = mockk<User>(relaxed = true)
        every { user.id } returns id
        every { user.isAdmin() } returns admin
        every { user.phone } returns phone
        every { user.email } returns email
        return user
    }

    private fun mockStatus(code: String): ApplicationStatus {
        val status = mockk<ApplicationStatus>(relaxed = true)
        every { status.code } returns code
        return status
    }

    private fun mockListing(
        id: Long = 10L,
        ownerId: Long = 1L,
        statusCode: String = ListingStatusCodes.ACTIVE,
    ): Listing {
        val listing = mockk<Listing>(relaxed = true)
        val status = mockk<ListingStatus>(relaxed = true)
        every { status.code } returns statusCode
        every { listing.id } returns id
        every { listing.status } returns status
        every { listing.postedBy } returns mockUser(id = ownerId)
        return listing
    }

    private fun mockApplication(
        id: Long = 100L,
        listing: Listing = mockListing(),
        applicant: User = mockUser(id = 2L),
        statusCode: String = ApplicationStatusCodes.SUBMITTED,
    ): AdoptionApplication {
        val app = mockk<AdoptionApplication>(relaxed = true)
        every { app.id } returns id
        every { app.listing } returns listing
        every { app.applicant } returns applicant
        every { app.status } returns mockStatus(statusCode)
        return app
    }

    @Test
    fun `submitApplication throws when listing not found`() {
        every { listingRepository.findById(99L) } returns Optional.empty()
        val result = service.submitApplication(99L, CreateApplicationRequest(), mockUser(id = 2L))
        assertIs<SubmitApplicationResult.NotFound>(result)
    }

    @Test
    fun `submitApplication throws conflict when listing is not ACTIVE`() {
        val listing = mockListing(id = 10L, ownerId = 1L, statusCode = ListingStatusCodes.DRAFT)
        every { listingRepository.findById(10L) } returns Optional.of(listing)
        val result = service.submitApplication(10L, CreateApplicationRequest(), mockUser(id = 2L))
        assertIs<SubmitApplicationResult.Conflict>(result)
    }

    @Test
    fun `submitApplication forbids applying to your own listing`() {
        val listing = mockListing(id = 10L, ownerId = 7L)
        every { listingRepository.findById(10L) } returns Optional.of(listing)
        val result = service.submitApplication(10L, CreateApplicationRequest(), mockUser(id = 7L))
        assertIs<SubmitApplicationResult.Forbidden>(result)
    }

    @Test
    fun `submitApplication throws conflict when applicant already has a pending application`() {
        val listing = mockListing(id = 10L, ownerId = 1L)
        every { listingRepository.findById(10L) } returns Optional.of(listing)
        every {
            applicationRepository.findByListing_IdAndApplicant_IdAndStatus_CodeInAndDeletedAtIsNull(
                10L,
                2L,
                ApplicationStatusCodes.PENDING_STATUSES,
            )
        } returns listOf(mockApplication())
        val result = service.submitApplication(10L, CreateApplicationRequest(), mockUser(id = 2L))
        assertIs<SubmitApplicationResult.Conflict>(result)
    }

    @Test
    fun `submitApplication throws when SUBMITTED status is missing`() {
        val listing = mockListing(id = 10L, ownerId = 1L)
        every { listingRepository.findById(10L) } returns Optional.of(listing)
        every {
            applicationRepository.findByListing_IdAndApplicant_IdAndStatus_CodeInAndDeletedAtIsNull(any(), any(), any())
        } returns emptyList()
        every { applicationStatusRepository.findByCode(ApplicationStatusCodes.SUBMITTED) } returns null
        assertFailsWith<IllegalStateException> {
            service.submitApplication(10L, CreateApplicationRequest(), mockUser(id = 2L))
        }
    }

    @Test
    fun `submitApplication saves with SUBMITTED status and falls back to user contact details`() {
        val listing = mockListing(id = 10L, ownerId = 1L)
        val applicant = mockUser(id = 2L, phone = "071222333", email = "applicant@mail.test")
        val submitted = mockStatus(ApplicationStatusCodes.SUBMITTED)
        every { listingRepository.findById(10L) } returns Optional.of(listing)
        every {
            applicationRepository.findByListing_IdAndApplicant_IdAndStatus_CodeInAndDeletedAtIsNull(any(), any(), any())
        } returns emptyList()
        every { applicationStatusRepository.findByCode(ApplicationStatusCodes.SUBMITTED) } returns submitted
        val savedMock = mockk<AdoptionApplication>(relaxed = true)
        every { savedMock.id } returns 500L
        every { applicationRepository.save(any()) } returns savedMock
        every { applicationRepository.findByIdWithAllAssociations(500L) } returns savedMock

        val result = service.submitApplication(10L, CreateApplicationRequest(message = "Hi"), applicant)

        val slot = slot<AdoptionApplication>()
        verify { applicationRepository.save(capture(slot)) }
        assertSame(submitted, slot.captured.status)
        assertSame(applicant, slot.captured.applicant)
        assertEquals("Hi", slot.captured.message)
        assertEquals("071222333", slot.captured.contactPhone)
        assertEquals("applicant@mail.test", slot.captured.contactEmail)
        assertIs<SubmitApplicationResult.Success>(result)
        assertSame(savedMock, result.application)
    }

    @Test
    fun `submitApplication uses request contact details when provided`() {
        val listing = mockListing(id = 10L, ownerId = 1L)
        every { listingRepository.findById(10L) } returns Optional.of(listing)
        every {
            applicationRepository.findByListing_IdAndApplicant_IdAndStatus_CodeInAndDeletedAtIsNull(any(), any(), any())
        } returns emptyList()
        every { applicationStatusRepository.findByCode(ApplicationStatusCodes.SUBMITTED) } returns
            mockStatus(ApplicationStatusCodes.SUBMITTED)
        val savedMock = mockk<AdoptionApplication>(relaxed = true)
        every { savedMock.id } returns 501L
        every { applicationRepository.save(any()) } returns savedMock
        every { applicationRepository.findByIdWithAllAssociations(501L) } returns savedMock

        service.submitApplication(
            10L,
            CreateApplicationRequest(contactPhone = "999", contactEmail = "override@mail.test"),
            mockUser(id = 2L),
        )

        val slot = slot<AdoptionApplication>()
        verify { applicationRepository.save(capture(slot)) }
        assertEquals("999", slot.captured.contactPhone)
        assertEquals("override@mail.test", slot.captured.contactEmail)
    }

    @Test
    fun `listApplicationsForListing throws when listing not found`() {
        every { listingRepository.findById(99L) } returns Optional.empty()
        val result = service.listApplicationsForListing(99L, mockUser(), PageRequest.of(0, 10))
        assertIs<ListApplicationsForListingResult.NotFound>(result)
    }

    @Test
    fun `listApplicationsForListing forbids a non-owner non-admin`() {
        every { listingRepository.findById(10L) } returns Optional.of(mockListing(id = 10L, ownerId = 1L))
        val result = service.listApplicationsForListing(10L, mockUser(id = 2L), PageRequest.of(0, 10))
        assertIs<ListApplicationsForListingResult.Forbidden>(result)
    }

    @Test
    fun `listApplicationsForListing allows the owner`() {
        val pageable = PageRequest.of(0, 10)
        every { listingRepository.findById(10L) } returns Optional.of(mockListing(id = 10L, ownerId = 1L))
        every { applicationRepository.findByListing_IdAndDeletedAtIsNull(10L, pageable) } returns PageImpl(emptyList())
        service.listApplicationsForListing(10L, mockUser(id = 1L), pageable)
        verify { applicationRepository.findByListing_IdAndDeletedAtIsNull(10L, pageable) }
    }

    @Test
    fun `listApplicationsForListing allows an admin who is not the owner`() {
        val pageable = PageRequest.of(0, 10)
        every { listingRepository.findById(10L) } returns Optional.of(mockListing(id = 10L, ownerId = 1L))
        every { applicationRepository.findByListing_IdAndDeletedAtIsNull(10L, pageable) } returns PageImpl(emptyList())
        service.listApplicationsForListing(10L, mockUser(id = 99L, admin = true), pageable)
        verify { applicationRepository.findByListing_IdAndDeletedAtIsNull(10L, pageable) }
    }

    @Test
    fun `reviewApplication throws when application not found`() {
        every { applicationRepository.findByIdWithAllAssociations(99L) } returns null
        val result = service.reviewApplication(99L, ApplicationDecision.APPROVE, mockUser())
        assertIs<ReviewApplicationResult.NotFound>(result)
    }

    @Test
    fun `reviewApplication forbids a reviewer who is neither listing owner nor admin`() {
        val app = mockApplication(id = 100L, listing = mockListing(id = 10L, ownerId = 1L))
        every { applicationRepository.findByIdWithAllAssociations(100L) } returns app
        val result = service.reviewApplication(100L, ApplicationDecision.APPROVE, mockUser(id = 2L))
        assertIs<ReviewApplicationResult.Forbidden>(result)
    }

    @Test
    fun `reviewApplication throws conflict when application is not pending`() {
        val app =
            mockApplication(
                id = 100L,
                listing = mockListing(id = 10L, ownerId = 1L),
                statusCode = ApplicationStatusCodes.APPROVED,
            )
        every { applicationRepository.findByIdWithAllAssociations(100L) } returns app
        val result = service.reviewApplication(100L, ApplicationDecision.REJECT, mockUser(id = 1L))
        assertIs<ReviewApplicationResult.Conflict>(result)
    }

    @Test
    fun `reviewApplication APPROVE marks listing adopted and auto-rejects other pending applications`() {
        val listing = mockListing(id = 10L, ownerId = 1L)
        val app = mockApplication(id = 100L, listing = listing)
        val other = mockk<AdoptionApplication>(relaxed = true)
        every { other.id } returns 200L
        val approved = mockStatus(ApplicationStatusCodes.APPROVED)
        val rejected = mockStatus(ApplicationStatusCodes.REJECTED)
        val reviewer = mockUser(id = 1L)

        every { applicationRepository.findByIdWithAllAssociations(100L) } returns app
        every { applicationStatusRepository.findByCode(ApplicationStatusCodes.APPROVED) } returns approved
        every { applicationStatusRepository.findByCode(ApplicationStatusCodes.REJECTED) } returns rejected
        every { listingService.markAdopted(listing) } just runs
        every {
            applicationRepository.findByListing_IdAndStatus_CodeInAndDeletedAtIsNull(10L, ApplicationStatusCodes.PENDING_STATUSES)
        } returns listOf(app, other)
        every { applicationRepository.saveAll(any<List<AdoptionApplication>>()) } returns emptyList()
        every { applicationRepository.save(app) } returns app

        service.reviewApplication(100L, ApplicationDecision.APPROVE, reviewer)

        verify { listingService.markAdopted(listing) }
        verify { app.status = approved }
        verify { other.status = rejected }
        verify { other.reviewedBy = reviewer }
        val slot = slot<List<AdoptionApplication>>()
        verify { applicationRepository.saveAll(capture(slot)) }
        assertEquals(listOf(other), slot.captured)
        verify { applicationRepository.save(app) }
    }

    @Test
    fun `reviewApplication REJECT sets rejected status and does not touch the listing`() {
        val listing = mockListing(id = 10L, ownerId = 1L)
        val app = mockApplication(id = 100L, listing = listing)
        val rejected = mockStatus(ApplicationStatusCodes.REJECTED)
        every { applicationRepository.findByIdWithAllAssociations(100L) } returns app
        every { applicationStatusRepository.findByCode(ApplicationStatusCodes.REJECTED) } returns rejected
        every { applicationRepository.save(app) } returns app

        service.reviewApplication(100L, ApplicationDecision.REJECT, mockUser(id = 1L))

        verify { app.status = rejected }
        verify(exactly = 0) { listingService.markAdopted(any()) }
        verify(exactly = 0) { applicationRepository.saveAll(any<List<AdoptionApplication>>()) }
    }

    @Test
    fun `reviewApplication throws when APPROVED status is missing`() {
        val app = mockApplication(id = 100L, listing = mockListing(id = 10L, ownerId = 1L))
        every { applicationRepository.findByIdWithAllAssociations(100L) } returns app
        every { applicationStatusRepository.findByCode(ApplicationStatusCodes.APPROVED) } returns null
        assertFailsWith<IllegalStateException> {
            service.reviewApplication(100L, ApplicationDecision.APPROVE, mockUser(id = 1L))
        }
    }

    @Test
    fun `withdrawApplication throws when application not found`() {
        every { applicationRepository.findByIdWithAllAssociations(99L) } returns null
        val result = service.withdrawApplication(99L, mockUser())
        assertIs<WithdrawApplicationResult.NotFound>(result)
    }

    @Test
    fun `withdrawApplication forbids anyone other than the applicant`() {
        val app = mockApplication(id = 100L, applicant = mockUser(id = 2L))
        every { applicationRepository.findByIdWithAllAssociations(100L) } returns app
        val result = service.withdrawApplication(100L, mockUser(id = 3L))
        assertIs<WithdrawApplicationResult.Forbidden>(result)
    }

    @Test
    fun `withdrawApplication throws conflict when application is no longer pending`() {
        val app =
            mockApplication(
                id = 100L,
                applicant = mockUser(id = 2L),
                statusCode = ApplicationStatusCodes.REJECTED,
            )
        every { applicationRepository.findByIdWithAllAssociations(100L) } returns app
        val result = service.withdrawApplication(100L, mockUser(id = 2L))
        assertIs<WithdrawApplicationResult.Conflict>(result)
    }

    @Test
    fun `withdrawApplication sets WITHDRAWN status for the applicant`() {
        val applicant = mockUser(id = 2L)
        val app = mockApplication(id = 100L, applicant = applicant)
        val withdrawn = mockStatus(ApplicationStatusCodes.WITHDRAWN)
        every { applicationRepository.findByIdWithAllAssociations(100L) } returns app
        every { applicationStatusRepository.findByCode(ApplicationStatusCodes.WITHDRAWN) } returns withdrawn
        every { applicationRepository.save(app) } returns app

        service.withdrawApplication(100L, applicant)

        verify { app.status = withdrawn }
        verify { applicationRepository.save(app) }
    }

    @Test
    fun `listMyApplications delegates to the repository`() {
        val pageable = PageRequest.of(0, 5)
        every { applicationRepository.findByApplicant_IdAndDeletedAtIsNull(2L, pageable) } returns PageImpl(emptyList())
        service.listMyApplications(mockUser(id = 2L), pageable)
        verify { applicationRepository.findByApplicant_IdAndDeletedAtIsNull(2L, pageable) }
    }

    @Test
    fun `adminListApplications delegates to the repository with the status filter`() {
        val pageable = PageRequest.of(0, 5)
        every { applicationRepository.findAllWithAssociations("APPROVED", pageable) } returns PageImpl(emptyList())
        service.adminListApplications("APPROVED", pageable)
        verify { applicationRepository.findAllWithAssociations("APPROVED", pageable) }
    }
}
