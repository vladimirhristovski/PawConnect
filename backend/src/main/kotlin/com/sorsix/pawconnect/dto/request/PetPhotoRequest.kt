package com.sorsix.pawconnect.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class PetPhotoRequest(
    @field:NotBlank @field:Size(max = 500) val url: String,
    val isPrimary: Boolean = false,
    val displayOrder: Int = 0
)