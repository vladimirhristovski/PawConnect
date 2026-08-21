package com.sorsix.pawconnect.util

object ApplicationStatusCodes {
    const val SUBMITTED = "SUBMITTED"
    const val UNDER_REVIEW = "UNDER_REVIEW"
    const val APPROVED = "APPROVED"
    const val REJECTED = "REJECTED"
    const val WITHDRAWN = "WITHDRAWN"
    const val CLOSED = "CLOSED"

    val PENDING_STATUSES = setOf(SUBMITTED, UNDER_REVIEW)
}