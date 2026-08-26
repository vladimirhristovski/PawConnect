package com.sorsix.pawconnect.service

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Service
@Profile("test")
class NoOpGeocodingService : GeocodingService {
    override fun geocode(query: String): GeoCoordinates? = null
}
