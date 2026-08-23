package com.sorsix.pawconnect.dto.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Size
import java.math.BigDecimal

data class UpdateBusinessRequest(
    @field:Size(min = 1) val typeCode: String? = null,
    @field:Size(min = 1) val name: String? = null,
    @field:Size(min = 1) val description: String? = null,
    @field:Size(min = 1) val phone: String? = null,
    @field:Email @field:Size(min = 1) val email: String? = null,
    @field:Size(min = 1) val address: String? = null,
    @field:Size(min = 1) val municipalityCode: String? = null,
    val latitude: BigDecimal? = null,
    val longitude: BigDecimal? = null
)