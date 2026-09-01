package com.sorsix.pawconnect.dto.request

import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.math.BigDecimal

data class CreateBusinessRequest(
    @field:NotBlank val typeCode: String,
    @field:NotBlank val name: String,
    val description: String? = null,
    @field:NotBlank val phone: String,
    @field:Email @field:Size(max = 255) val email: String? = null,
    @field:NotBlank val address: String,
    @field:NotBlank val municipalityCode: String,
    val latitude: BigDecimal? = null,
    val longitude: BigDecimal? = null,
    @field:Valid val photos: List<BusinessPhotoRequest> = emptyList()
)