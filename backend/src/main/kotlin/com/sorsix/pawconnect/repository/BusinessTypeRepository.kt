package com.sorsix.pawconnect.repository

import com.sorsix.pawconnect.model.BusinessType
import org.springframework.data.jpa.repository.JpaRepository

interface BusinessTypeRepository : JpaRepository<BusinessType, Long> {
    fun findByCode(code: String): BusinessType?
}