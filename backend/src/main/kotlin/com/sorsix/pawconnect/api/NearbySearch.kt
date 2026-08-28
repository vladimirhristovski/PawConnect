package com.sorsix.pawconnect.api

import java.math.BigDecimal

data class NearbySearch(val lat: BigDecimal, val lng: BigDecimal, val radiusKm: Double)

fun resolveNearbySearch(lat: BigDecimal?, lng: BigDecimal?, radiusKm: Double?): NearbySearch? {
    if (lat == null && lng == null && radiusKm == null) return null
    if (lat == null || lng == null || radiusKm == null) {
        throw IllegalArgumentException("lat, lng, and radiusKm must all be provided together")
    }
    return NearbySearch(lat, lng, radiusKm)
}
