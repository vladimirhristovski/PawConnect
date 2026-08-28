package com.sorsix.pawconnect.repository

import com.sorsix.pawconnect.domain.ApplicationStatus
import org.springframework.data.jpa.repository.JpaRepository

interface ApplicationStatusRepository : JpaRepository<ApplicationStatus, Long> {
    fun findByCode(code: String): ApplicationStatus?
}