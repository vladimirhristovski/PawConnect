package com.sorsix.pawconnect.dto.response

import com.sorsix.pawconnect.domain.Municipality

data class MunicipalityResponse(
    val code: String,
    val name: String,
    val cityCode: String?,
) {
    companion object {
        fun from(municipality: Municipality) = MunicipalityResponse(municipality.code, municipality.name, municipality.city?.code)
    }
}
