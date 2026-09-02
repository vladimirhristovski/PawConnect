package com.sorsix.pawconnect.service

import com.sorsix.pawconnect.common.ApplicationStatusCodes
import com.sorsix.pawconnect.common.ListingStatusCodes
import com.sorsix.pawconnect.common.requireId
import com.sorsix.pawconnect.domain.Listing
import com.sorsix.pawconnect.repository.AdoptionApplicationRepository
import com.sorsix.pawconnect.repository.ApplicationStatusRepository
import com.sorsix.pawconnect.repository.ListingRepository
import com.sorsix.pawconnect.repository.ListingStatusRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Component
class ListingExpiryJob(
    private val listingRepository: ListingRepository,
    private val listingStatusRepository: ListingStatusRepository,
    private val adoptionApplicationRepository: AdoptionApplicationRepository,
    private val applicationStatusRepository: ApplicationStatusRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = "\${app.listings.expiry-check-interval}")
    @Transactional
    fun expireOverdueListings() {
        val overdue = listingRepository.findByStatus_CodeAndExpiresAtBefore(ListingStatusCodes.ACTIVE, Instant.now())
        if (overdue.isEmpty()) return

        val expiredStatus = listingStatusRepository.findByCode(ListingStatusCodes.EXPIRED)
            ?: throw IllegalStateException("EXPIRED status not found")
        overdue.forEach { it.status = expiredStatus }
        listingRepository.saveAll(overdue)

        val rejectedApplications = rejectPendingApplications(overdue)
        log.info(
            "Listing expiry: {} listing(s) marked EXPIRED, {} pending application(s) rejected",
            overdue.size, rejectedApplications
        )
    }

    private fun rejectPendingApplications(expiredListings: List<Listing>): Int {
        val pendingApps = expiredListings.flatMap { listing ->
            adoptionApplicationRepository.findByListing_IdAndStatus_CodeInAndDeletedAtIsNull(
                listing.requireId(), ApplicationStatusCodes.PENDING_STATUSES
            )
        }
        if (pendingApps.isEmpty()) return 0

        val rejectedStatus = applicationStatusRepository.findByCode(ApplicationStatusCodes.REJECTED)
            ?: throw IllegalStateException("REJECTED status not found")
        val now = Instant.now()
        pendingApps.forEach { app ->
            app.status = rejectedStatus
            app.reviewedAt = now
        }
        adoptionApplicationRepository.saveAll(pendingApps)
        return pendingApps.size
    }
}
