package com.sorsix.pawconnect.dto.request

data class CreateApplicationRequest(
    val message: String? = null,
    val contactPhone: String? = null,
    val contactEmail: String? = null
)