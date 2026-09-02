package com.sorsix.pawconnect.service

import com.sorsix.pawconnect.common.ListingStatusCodes
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
    private val listingStatusRepository: ListingStatusRepository
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
        log.info("Listing expiry: {} listing(s) marked EXPIRED", overdue.size)
    }
}
