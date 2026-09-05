package com.sorsix.pawconnect.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class PetMatcherRequest(
    @field:NotBlank(message = "Prompt is required")
    @field:Size(min = 5, max = 2000, message = "Please keep your description between 5 and 2000 characters")
    val prompt: String,
)