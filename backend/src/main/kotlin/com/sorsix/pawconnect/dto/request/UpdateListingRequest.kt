package com.sorsix.pawconnect.dto.request

import jakarta.validation.constraints.Future
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.Instant

data class UpdateListingRequest(
    @field:Size(max = 150) val title: String? = null,
    @field:Size(max = 5000) val description: String? = null,
    @field:PositiveOrZero val adoptionFee: BigDecimal? = null,
    val municipalityCode: String? = null,
    val latitude: BigDecimal? = null,
    val longitude: BigDecimal? = null,
    @field:Future val expiresAt: Instant? = null,
)
