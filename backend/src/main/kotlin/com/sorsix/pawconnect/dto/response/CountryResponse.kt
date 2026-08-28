package com.sorsix.pawconnect.dto.response

import com.sorsix.pawconnect.domain.Country

data class CountryResponse(
    val code: String, val name: String
) {
    companion object {
        fun from(country: Country) = CountryResponse(country.code, country.name)
    }
}