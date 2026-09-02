package com.sorsix.pawconnect.service

import com.sorsix.pawconnect.common.ApplicationStatusCodes
import com.sorsix.pawconnect.common.ListingStatusCodes
import com.sorsix.pawconnect.domain.AdoptionApplication
import com.sorsix.pawconnect.domain.ApplicationStatus
import com.sorsix.pawconnect.domain.Listing
import com.sorsix.pawconnect.domain.ListingStatus
import com.sorsix.pawconnect.repository.AdoptionApplicationRepository
import com.sorsix.pawconnect.repository.ApplicationStatusRepository
import com.sorsix.pawconnect.repository.ListingRepository
import com.sorsix.pawconnect.repository.ListingStatusRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ListingExpiryJobTest {

    private val listingRepository = mockk<ListingRepository>()
    private val listingStatusRepository = mockk<ListingStatusRepository>()
    private val adoptionApplicationRepository = mockk<AdoptionApplicationRepository>()
    private val applicationStatusRepository = mockk<ApplicationStatusRepository>()
    private lateinit var job: ListingExpiryJob

    @BeforeEach
    fun setup() {
        job = ListingExpiryJob(listingRepository, listingStatusRepository, adoptionApplicationRepository, applicationStatusRepository)
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

    private fun mockListing(id: Long): Listing {
        val listing = mockk<Listing>(relaxed = true)
        every { listing.id } returns id
        return listing
    }

    @Test
    fun `does nothing when no listings are overdue`() {
        every { listingRepository.findByStatus_CodeAndExpiresAtBefore(ListingStatusCodes.ACTIVE, any()) } returns emptyList()

        job.expireOverdueListings()

        verify(exactly = 0) { listingStatusRepository.findByCode(any()) }
        verify(exactly = 0) { listingRepository.saveAll(any<List<Listing>>()) }
        verify(exactly = 0) { adoptionApplicationRepository.findByListing_IdAndStatus_CodeInAndDeletedAtIsNull(any(), any()) }
    }

    @Test
    fun `marks overdue active listings as expired`() {
        val listing = mockListing(id = 10L)
        every { listingRepository.findByStatus_CodeAndExpiresAtBefore(ListingStatusCodes.ACTIVE, any()) } returns listOf(listing)
        val expired = mockListingStatus(ListingStatusCodes.EXPIRED)
        every { listingStatusRepository.findByCode(ListingStatusCodes.EXPIRED) } returns expired
        every { listingRepository.saveAll(any<List<Listing>>()) } returns listOf(listing)
        every {
            adoptionApplicationRepository.findByListing_IdAndStatus_CodeInAndDeletedAtIsNull(10L, ApplicationStatusCodes.PENDING_STATUSES)
        } returns emptyList()

        job.expireOverdueListings()

        verify { listing.status = expired }
        verify { listingRepository.saveAll(listOf(listing)) }
    }

    @Test
    fun `throws when the EXPIRED status row is missing`() {
        val listing = mockListing(id = 10L)
        every { listingRepository.findByStatus_CodeAndExpiresAtBefore(ListingStatusCodes.ACTIVE, any()) } returns listOf(listing)
        every { listingStatusRepository.findByCode(ListingStatusCodes.EXPIRED) } returns null

        assertFailsWith<IllegalStateException> {
            job.expireOverdueListings()
        }
    }

    @Test
    fun `rejects pending applications on an expired listing`() {
        val listing = mockListing(id = 10L)
        every { listingRepository.findByStatus_CodeAndExpiresAtBefore(ListingStatusCodes.ACTIVE, any()) } returns listOf(listing)
        every { listingStatusRepository.findByCode(ListingStatusCodes.EXPIRED) } returns mockListingStatus(ListingStatusCodes.EXPIRED)
        every { listingRepository.saveAll(any<List<Listing>>()) } returns listOf(listing)

        val application = mockk<AdoptionApplication>(relaxed = true)
        every {
            adoptionApplicationRepository.findByListing_IdAndStatus_CodeInAndDeletedAtIsNull(10L, ApplicationStatusCodes.PENDING_STATUSES)
        } returns listOf(application)
        val rejected = mockAppStatus(ApplicationStatusCodes.REJECTED)
        every { applicationStatusRepository.findByCode(ApplicationStatusCodes.REJECTED) } returns rejected
        every { adoptionApplicationRepository.saveAll(any<List<AdoptionApplication>>()) } returns listOf(application)

        job.expireOverdueListings()

        verify { application.status = rejected }
        verify { application.reviewedAt = any() }
        verify { adoptionApplicationRepository.saveAll(listOf(application)) }
    }

    @Test
    fun `leaves reviewedBy null since expiry is not reviewed by anyone`() {
        val listing = mockListing(id = 10L)
        every { listingRepository.findByStatus_CodeAndExpiresAtBefore(ListingStatusCodes.ACTIVE, any()) } returns listOf(listing)
        every { listingStatusRepository.findByCode(ListingStatusCodes.EXPIRED) } returns mockListingStatus(ListingStatusCodes.EXPIRED)
        every { listingRepository.saveAll(any<List<Listing>>()) } returns listOf(listing)

        val application = mockk<AdoptionApplication>(relaxed = true)
        every {
            adoptionApplicationRepository.findByListing_IdAndStatus_CodeInAndDeletedAtIsNull(10L, ApplicationStatusCodes.PENDING_STATUSES)
        } returns listOf(application)
        every { applicationStatusRepository.findByCode(ApplicationStatusCodes.REJECTED) } returns mockAppStatus(ApplicationStatusCodes.REJECTED)
        every { adoptionApplicationRepository.saveAll(any<List<AdoptionApplication>>()) } returns listOf(application)

        job.expireOverdueListings()

        verify(exactly = 0) { application.reviewedBy = any() }
    }
}
