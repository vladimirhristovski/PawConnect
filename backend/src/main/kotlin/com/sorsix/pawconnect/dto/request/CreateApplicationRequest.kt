package com.sorsix.pawconnect.dto.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Size

data class CreateApplicationRequest(
    @field:Size(max = 2000) val message: String? = null,
    @field:Size(max = 30) val contactPhone: String? = null,
    @field:Email @field:Size(max = 255) val contactEmail: String? = null
)