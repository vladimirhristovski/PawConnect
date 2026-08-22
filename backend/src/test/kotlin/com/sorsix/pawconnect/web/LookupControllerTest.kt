package com.sorsix.pawconnect.web

import com.sorsix.pawconnect.TestcontainersConfiguration
import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.restassured.module.kotlin.extensions.*
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class LookupControllerTest {

    @LocalServerPort
    private var port: Int = 0

    @BeforeEach
    fun setup() {
        RestAssured.port = port
    }

    @Test
    fun `get species returns list`() {
        When {
            get("/api/lookups/species")
        } Then {
            statusCode(200)
            body("size()", greaterThan(0))
            body("[0].code", notNullValue())
        }
    }

    @Test
    fun `get breeds with species code returns filtered`() {
        val speciesResponse = Given {
            accept(ContentType.JSON)
        } When {
            get("/api/lookups/species")
        } Then {
            statusCode(200)
        }
        val speciesCode = speciesResponse.extract().jsonPath().getString("[0].code")

        Given {
            queryParam("speciesCode", speciesCode)
        } When {
            get("/api/lookups/breeds")
        } Then {
            statusCode(200)
            body("size()", greaterThanOrEqualTo(0))
        }
    }

    @Test
    fun `get breeds without species returns all`() {
        When {
            get("/api/lookups/breeds")
        } Then {
            statusCode(200)
            body("size()", greaterThan(0))
        }
    }

    @Test
    fun `get business types returns list`() {
        When {
            get("/api/lookups/business-types")
        } Then {
            statusCode(200)
            body("size()", greaterThan(0))
        }
    }

    @Test
    fun `get countries returns list`() {
        When {
            get("/api/lookups/countries")
        } Then {
            statusCode(200)
            body("size()", greaterThan(0))
        }
    }

    @Test
    fun `get cities with country code returns filtered`() {
        val countryResponse = Given {
            accept(ContentType.JSON)
        } When {
            get("/api/lookups/countries")
        } Then {
            statusCode(200)
        }
        val countryCode = countryResponse.extract().jsonPath().getString("[0].code")

        Given {
            queryParam("countryCode", countryCode)
        } When {
            get("/api/lookups/cities")
        } Then {
            statusCode(200)
            body("size()", greaterThanOrEqualTo(0))
        }
    }

    @Test
    fun `get municipalities with city code returns filtered`() {
        val cityResponse = Given {
            accept(ContentType.JSON)
        } When {
            get("/api/lookups/cities")
        } Then {
            statusCode(200)
        }
        val cityCode = cityResponse.extract().jsonPath().getString("[0].code")

        Given {
            queryParam("cityCode", cityCode)
        } When {
            get("/api/lookups/municipalities")
        } Then {
            statusCode(200)
            body("size()", greaterThanOrEqualTo(0))
        }
    }
}