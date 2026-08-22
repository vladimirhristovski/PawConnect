package com.sorsix.pawconnect.dto.request

import java.math.BigDecimal
import java.time.Instant

data class UpdateListingRequest(
    val title: String? = null,
    val description: String? = null,
    val adoptionFee: BigDecimal? = null,
    val municipalityCode: String? = null,
    val latitude: BigDecimal? = null,
    val longitude: BigDecimal? = null,
    val expiresAt: Instant? = null
)