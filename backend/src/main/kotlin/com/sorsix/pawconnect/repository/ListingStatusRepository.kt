package com.sorsix.pawconnect.repository

import com.sorsix.pawconnect.domain.ListingStatus
import org.springframework.data.jpa.repository.JpaRepository

interface ListingStatusRepository : JpaRepository<ListingStatus, Long> {
    fun findByCode(code: String): ListingStatus?
}