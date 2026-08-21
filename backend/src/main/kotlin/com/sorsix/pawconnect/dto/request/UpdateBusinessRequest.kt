package com.sorsix.pawconnect.dto.request

import java.math.BigDecimal

data class UpdateBusinessRequest(
    val typeCode: String? = null,
    val name: String? = null,
    val description: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val municipalityCode: String? = null,
    val latitude: BigDecimal? = null,
    val longitude: BigDecimal? = null
)