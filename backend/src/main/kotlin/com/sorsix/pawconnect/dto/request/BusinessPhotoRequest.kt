package com.sorsix.pawconnect.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class BusinessPhotoRequest(
    @field:NotBlank @field:Size(max = 1024) val url: String,
    val isPrimary: Boolean = false,
    val displayOrder: Int? = null,
)
