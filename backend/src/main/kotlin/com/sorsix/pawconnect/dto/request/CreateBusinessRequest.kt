package com.sorsix.pawconnect.dto.request

import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.math.BigDecimal

data class CreateBusinessRequest(
    @field:NotBlank @field:Size(max = 40) val typeCode: String,
    @field:NotBlank @field:Size(max = 150) val name: String,
    @field:Size(max = 5000) val description: String? = null,
    @field:NotBlank @field:Size(max = 30) val phone: String,
    @field:Email @field:Size(max = 255) val email: String? = null,
    @field:NotBlank @field:Size(max = 255) val address: String,
    @field:NotBlank @field:Size(max = 40) val municipalityCode: String,
    val latitude: BigDecimal? = null,
    val longitude: BigDecimal? = null,
    @field:Valid val photos: List<BusinessPhotoRequest> = emptyList(),
)
