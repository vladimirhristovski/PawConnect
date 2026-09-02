package com.sorsix.pawconnect.dto.request

import jakarta.validation.Valid
import jakarta.validation.constraints.Future
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.Instant

data class CreateListingRequest(
    val petId: Long? = null,
    @field:Valid val pet: CreatePetRequest? = null,
    val businessId: Long? = null,
    @field:NotBlank val municipalityCode: String,
    @field:Size(max = 150) val title: String? = null,
    @field:Size(max = 5000) val description: String? = null,
    @field:PositiveOrZero val adoptionFee: BigDecimal = BigDecimal.ZERO,
    val latitude: BigDecimal? = null,
    val longitude: BigDecimal? = null,
    @field:Future val expiresAt: Instant? = null,
    val saveAsDraft: Boolean = false
)