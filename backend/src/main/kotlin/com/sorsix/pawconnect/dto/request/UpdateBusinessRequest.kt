package com.sorsix.pawconnect.dto.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Size
import java.math.BigDecimal

data class UpdateBusinessRequest(
    @field:Size(min = 1, max = 40) val typeCode: String? = null,
    @field:Size(min = 1, max = 150) val name: String? = null,
    @field:Size(min = 1, max = 5000) val description: String? = null,
    @field:Size(min = 1, max = 30) val phone: String? = null,
    @field:Email @field:Size(min = 1, max = 255) val email: String? = null,
    @field:Size(min = 1, max = 255) val address: String? = null,
    @field:Size(min = 1, max = 40) val municipalityCode: String? = null,
    val latitude: BigDecimal? = null,
    val longitude: BigDecimal? = null
)