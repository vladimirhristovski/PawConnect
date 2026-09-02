package com.sorsix.pawconnect.service

import com.sorsix.pawconnect.common.ListingStatusCodes
import com.sorsix.pawconnect.domain.Listing
import com.sorsix.pawconnect.domain.ListingStatus
import com.sorsix.pawconnect.repository.ListingRepository
import com.sorsix.pawconnect.repository.ListingStatusRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class ListingExpiryJobTest {

    private val listingRepository = mockk<ListingRepository>()
    private val listingStatusRepository = mockk<ListingStatusRepository>()
    private lateinit var job: ListingExpiryJob

    @BeforeEach
    fun setup() {
        job = ListingExpiryJob(listingRepository, listingStatusRepository)
    }

    private fun mockStatus(code: String): ListingStatus {
        val status = mockk<ListingStatus>(relaxed = true)
        every { status.code } returns code
        return status
    }

    @Test
    fun `does nothing when no listings are overdue`() {
        every { listingRepository.findByStatus_CodeAndExpiresAtBefore(ListingStatusCodes.ACTIVE, any()) } returns emptyList()

        job.expireOverdueListings()

        verify(exactly = 0) { listingStatusRepository.findByCode(any()) }
        verify(exactly = 0) { listingRepository.saveAll(any<List<Listing>>()) }
    }

    @Test
    fun `marks overdue active listings as expired`() {
        val listing = mockk<Listing>(relaxed = true)
        every { listing.status = any() } answers { }
        every { listingRepository.findByStatus_CodeAndExpiresAtBefore(ListingStatusCodes.ACTIVE, any()) } returns listOf(listing)
        val expired = mockStatus(ListingStatusCodes.EXPIRED)
        every { listingStatusRepository.findByCode(ListingStatusCodes.EXPIRED) } returns expired
        every { listingRepository.saveAll(any<List<Listing>>()) } returns listOf(listing)

        job.expireOverdueListings()

        verify { listing.status = expired }
        verify { listingRepository.saveAll(listOf(listing)) }
    }

    @Test
    fun `throws when the EXPIRED status row is missing`() {
        val listing = mockk<Listing>(relaxed = true)
        every { listingRepository.findByStatus_CodeAndExpiresAtBefore(ListingStatusCodes.ACTIVE, any()) } returns listOf(listing)
        every { listingStatusRepository.findByCode(ListingStatusCodes.EXPIRED) } returns null

        assertFailsWith<IllegalStateException> {
            job.expireOverdueListings()
        }
    }
}
