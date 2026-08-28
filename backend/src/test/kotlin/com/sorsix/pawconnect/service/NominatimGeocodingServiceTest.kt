package com.sorsix.pawconnect.service

import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import java.math.BigDecimal

class NominatimGeocodingServiceTest {

    private fun buildService(): Pair<NominatimGeocodingService, MockRestServiceServer> {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val client = builder.baseUrl("https://nominatim.openstreetmap.org").build()
        return NominatimGeocodingService(client) to server
    }

    @Test
    fun `geocode returns coordinates from the first result`() {
        val (service, server) = buildService()

        server.expect(requestTo(containsString("/search")))
            .andExpect(method(org.springframework.http.HttpMethod.GET))
            .andRespond(
                withSuccess(
                    """[{"lat": "42.0000", "lon": "21.4000"}, {"lat": "1.0", "lon": "1.0"}]""",
                    MediaType.APPLICATION_JSON
                )
            )

        val result = service.geocode("Skopje, North Macedonia")

        assertEquals(BigDecimal("42.0000"), result?.latitude)
        assertEquals(BigDecimal("21.4000"), result?.longitude)
        server.verify()
    }

    @Test
    fun `geocode returns null when there are no results`() {
        val (service, server) = buildService()

        server.expect(requestTo(containsString("/search")))
            .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON))

        val result = service.geocode("Nowhere at all")

        assertNull(result)
        server.verify()
    }
}
