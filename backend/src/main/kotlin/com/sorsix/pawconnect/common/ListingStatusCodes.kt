package com.sorsix.pawconnect.common

object ListingStatusCodes {
    const val DRAFT = "DRAFT"
    const val ACTIVE = "ACTIVE"
    const val ADOPTED = "ADOPTED"
    const val EXPIRED = "EXPIRED"
    const val CANCELLED = "CANCELLED"

    val OPEN_STATUSES = setOf(DRAFT, ACTIVE)
    val VISIBLE_PUBLIC = setOf(ACTIVE, ADOPTED)
}
