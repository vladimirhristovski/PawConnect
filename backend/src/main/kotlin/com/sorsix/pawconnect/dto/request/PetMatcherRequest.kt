package com.sorsix.pawconnect.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class PetMatcherRequest(
    @field:NotBlank(message = "Prompt is required")
    @field:Size(min = 5, message = "Please describe your lifestyle in a bit more detail")
    val prompt: String,
)