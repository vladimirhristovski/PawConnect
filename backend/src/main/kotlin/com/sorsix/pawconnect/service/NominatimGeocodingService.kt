package com.sorsix.pawconnect.service

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.math.BigDecimal

@Service
@Profile("!test")
class NominatimGeocodingService(
    private val restClient: RestClient
) : GeocodingService {

    override fun geocode(query: String): GeoCoordinates? {
        val results = restClient.get()
            .uri { it.path("/search").queryParam("q", query).queryParam("format", "json").queryParam("limit", "1").build() }
            .retrieve()
            .body(Array<NominatimResult>::class.java)

        return results?.firstOrNull()?.let { GeoCoordinates(BigDecimal(it.lat), BigDecimal(it.lon)) }
    }

    private data class NominatimResult(val lat: String, val lon: String)
}
