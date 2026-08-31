package com.sorsix.pawconnect.dto.request

import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal

data class CreateBusinessRequest(
    @field:NotBlank val typeCode: String,
    @field:NotBlank val name: String,
    val description: String? = null,
    @field:NotBlank val phone: String,
    val email: String? = null,
    @field:NotBlank val address: String,
    @field:NotBlank val municipalityCode: String,
    val latitude: BigDecimal? = null,
    val longitude: BigDecimal? = null,
    val photos: List<BusinessPhotoRequest> = emptyList()
)