package com.sorsix.pawconnect.dto.response

import com.sorsix.pawconnect.model.City

data class CityResponse(
    val code: String, val name: String, val countryCode: String?
) {
    companion object {
        fun from(city: City) = CityResponse(city.code, city.name, city.country?.code)
    }
}