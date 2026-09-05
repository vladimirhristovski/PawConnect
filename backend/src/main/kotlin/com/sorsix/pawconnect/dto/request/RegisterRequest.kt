package com.sorsix.pawconnect.dto.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class RegisterRequest(
    @field:NotBlank @field:Size(min = 3, max = 100)
    val username: String,
    @field:NotBlank @field:Email @field:Size(max = 255)
    val email: String,
    @field:NotBlank @field:NotNull @field:Size(min = 6, max = 100) val password: String,
    @field:Size(max = 100)
    val firstName: String? = null,
    @field:Size(max = 100)
    val lastName: String? = null,
    @field:Size(max = 30)
    val phone: String? = null,
)
