package com.sorsix.pawconnect.service

import java.math.BigDecimal

data class GeoCoordinates(val latitude: BigDecimal, val longitude: BigDecimal)

interface GeocodingService {
    fun geocode(query: String): GeoCoordinates?
}
