package com.sorsix.pawconnect.common

import com.sorsix.pawconnect.domain.ApplicationStatus
import com.sorsix.pawconnect.domain.ListingStatus
import com.sorsix.pawconnect.domain.base.BaseEntity
import com.sorsix.pawconnect.repository.ApplicationStatusRepository
import com.sorsix.pawconnect.repository.ListingStatusRepository

fun BaseEntity.requireId(): Long = id ?: throw IllegalStateException("${javaClass.simpleName} has not been persisted yet")

fun ListingStatusRepository.requireByCode(code: String): ListingStatus =
    findByCode(code) ?: throw IllegalStateException("$code status not found")

fun ApplicationStatusRepository.requireByCode(code: String): ApplicationStatus =
    findByCode(code) ?: throw IllegalStateException("$code status not found")
