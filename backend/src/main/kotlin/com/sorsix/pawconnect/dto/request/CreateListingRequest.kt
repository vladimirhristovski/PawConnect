package com.sorsix.pawconnect.dto.request

import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal
import java.time.Instant

data class CreateListingRequest(
    val petId: Long? = null,
    val pet: CreatePetRequest? = null,
    val businessId: Long? = null,
    @field:NotBlank val municipalityCode: String,
    val title: String? = null,
    val description: String? = null,
    val adoptionFee: BigDecimal = BigDecimal.ZERO,
    val latitude: BigDecimal? = null,
    val longitude: BigDecimal? = null,
    val expiresAt: Instant? = null,
    val saveAsDraft: Boolean = false
)