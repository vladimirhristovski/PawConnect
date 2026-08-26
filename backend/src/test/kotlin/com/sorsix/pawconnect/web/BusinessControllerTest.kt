package com.sorsix.pawconnect.web

import com.sorsix.pawconnect.TestcontainersConfiguration
import com.sorsix.pawconnect.service.EmailService
import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.restassured.module.kotlin.extensions.Extract
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.math.BigDecimal
import javax.sql.DataSource

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class BusinessControllerTest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var dataSource: DataSource

    @MockitoBean
    private lateinit var emailService: EmailService

    private lateinit var ownerToken: String
    private lateinit var ownerUsername: String
    private var businessId: Long = 0L
    private lateinit var businessTypeCode: String
    private lateinit var municipalityCode: String

    @BeforeEach
    fun setup() {
        RestAssured.port = port
        cleanDatabase()
        prepareLookupCodes()
        prepareOwnerAndBusiness()
    }

    private fun cleanDatabase() {
        dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                statement.execute("SET session_replication_role = 'replica'")
                statement.execute(
                    """
                    TRUNCATE TABLE
                        refresh_tokens, password_reset_tokens, user_roles, users,
                        adoption_applications, businesses, listings, pet_photos, pets
                    RESTART IDENTITY CASCADE
                    """.trimIndent()
                )
                statement.execute("SET session_replication_role = 'origin'")
            }
        }
    }

    private fun prepareLookupCodes() {
        businessTypeCode = Given {
            accept(ContentType.JSON)
        } When {
            get("/api/lookups/business-types")
        } Then {
            statusCode(200)
        } Extract {
            jsonPath().getString("[0].code")
        }

        municipalityCode = Given {
            accept(ContentType.JSON)
        } When {
            get("/api/lookups/municipalities")
        } Then {
            statusCode(200)
        } Extract {
            jsonPath().getString("[0].code")
        }
    }

    private fun prepareOwnerAndBusiness() {
        val username = "bizowner_${System.currentTimeMillis()}"
        ownerUsername = username
        val email = "$username@test.com"

        Given {
            body(
                """
                {
                    "username": "$username",
                    "email": "$email",
                    "password": "Password1!",
                    "firstName": "Business",
                    "lastName": "Owner"
                }
                """.trimIndent()
            )
            contentType(ContentType.JSON)
        } When {
            post("/api/auth/register")
        } Then {
            statusCode(201)
        }

        val loginResponse = Given {
            body("""{"username":"$username","password":"Password1!"}""")
            contentType(ContentType.JSON)
        } When {
            post("/api/auth/login")
        } Then {
            statusCode(200)
        } Extract {
            jsonPath()
        }
        ownerToken = loginResponse.getString("accessToken")

        val createPayload = """
        {
            "name": "Test Business",
            "typeCode": "$businessTypeCode",
            "municipalityCode": "$municipalityCode",
            "address": "123 Main St",
            "phone": "123456789",
            "email": "business@test.com",
            "description": "A test business"
        }
        """.trimIndent()

        businessId = Given {
            header("Authorization", "Bearer $ownerToken")
            body(createPayload)
            contentType(ContentType.JSON)
        } When {
            post("/api/businesses")
        } Then {
            statusCode(201)
        } Extract {
            jsonPath().getLong("id")
        }
    }

    private fun createBusiness(
        token: String,
        typeCode: String = businessTypeCode,
        latitude: BigDecimal? = null,
        longitude: BigDecimal? = null
    ): Long {
        val name = "Business_${System.nanoTime()}"
        val locationFields = if (latitude != null && longitude != null) {
            """, "latitude": $latitude, "longitude": $longitude"""
        } else ""
        val payload = """
        {
            "name": "$name",
            "typeCode": "$typeCode",
            "municipalityCode": "$municipalityCode",
            "address": "1 Test St",
            "phone": "123456789",
            "email": "$name@test.com",
            "description": "desc"
            $locationFields
        }
        """.trimIndent()

        return Given {
            header("Authorization", "Bearer $token")
            body(payload)
            contentType(ContentType.JSON)
        } When {
            post("/api/businesses")
        } Then {
            statusCode(201)
        } Extract {
            jsonPath().getLong("id")
        }
    }

    @Test
    fun `search businesses near location returns only businesses within radius ordered by distance`() {
        val nearId = createBusiness(ownerToken, latitude = "42.0000".toBigDecimal(), longitude = "21.4000".toBigDecimal())
        val withinRadiusId =
            createBusiness(ownerToken, latitude = "42.0500".toBigDecimal(), longitude = "21.4000".toBigDecimal())
        val farId = createBusiness(ownerToken, latitude = "43.0000".toBigDecimal(), longitude = "21.4000".toBigDecimal())

        Given {
            queryParam("lat", "42.0000")
            queryParam("lng", "21.4000")
            queryParam("radiusKm", "10")
            queryParam("size", 20)
        } When {
            get("/api/businesses")
        } Then {
            statusCode(200)
            body("content*.id", hasItems(nearId.toInt(), withinRadiusId.toInt()))
            body("content*.id", not(hasItem(farId.toInt())))
            body("content[0].id", equalTo(nearId.toInt()))
        }
    }

    @Test
    fun `search businesses near location filters by type`() {
        val vetId = createBusiness(
            ownerToken, typeCode = "VET",
            latitude = "42.0".toBigDecimal(), longitude = "21.4".toBigDecimal()
        )
        val groomerId = createBusiness(
            ownerToken, typeCode = "GROOMER",
            latitude = "42.0".toBigDecimal(), longitude = "21.4".toBigDecimal()
        )

        Given {
            queryParam("lat", "42.0")
            queryParam("lng", "21.4")
            queryParam("radiusKm", "10")
            queryParam("typeCode", "VET")
            queryParam("size", 20)
        } When {
            get("/api/businesses")
        } Then {
            statusCode(200)
            body("content*.id", hasItem(vetId.toInt()))
            body("content*.id", not(hasItem(groomerId.toInt())))
        }
    }

    @Test
    fun `search businesses without precise coordinates are excluded from nearby results`() {
        Given {
            queryParam("lat", "42.0")
            queryParam("lng", "21.4")
            queryParam("radiusKm", "10")
            queryParam("size", 20)
        } When {
            get("/api/businesses")
        } Then {
            statusCode(200)
            body("content*.id", not(hasItem(businessId.toInt())))
        }
    }

    @Test
    fun `search businesses with only some nearby params returns 400`() {
        Given {
            queryParam("lat", "42.0")
            queryParam("radiusKm", "10")
        } When {
            get("/api/businesses")
        } Then {
            statusCode(400)
        }
    }

    @Test
    fun `create business returns 201 and business data`() {
        val newBusinessPayload = """
        {
            "name": "New Business",
            "typeCode": "$businessTypeCode",
            "municipalityCode": "$municipalityCode",
            "address": "456 Oak Ave",
            "phone": "987654321",
            "email": "new@business.com",
            "description": "Brand new business"
        }
        """.trimIndent()

        Given {
            header("Authorization", "Bearer $ownerToken")
            body(newBusinessPayload)
            contentType(ContentType.JSON)
        } When {
            post("/api/businesses")
        } Then {
            statusCode(201)
            body("id", notNullValue())
            body("name", equalTo("New Business"))
            body("typeCode", equalTo(businessTypeCode))
            body("municipalityCode", equalTo(municipalityCode))
            body("address", equalTo("456 Oak Ave"))
            body("phone", equalTo("987654321"))
            body("email", equalTo("new@business.com"))
            body("description", equalTo("Brand new business"))
            body("ownerUsername", equalTo(ownerUsername))
        }
    }

    @Test
    fun `create business with invalid data returns 400`() {
        val invalidPayload = """
        {
            "name": "",
            "typeCode": "$businessTypeCode",
            "municipalityCode": "$municipalityCode",
            "address": "",
            "phone": ""
        }
        """.trimIndent()

        Given {
            header("Authorization", "Bearer $ownerToken")
            body(invalidPayload)
            contentType(ContentType.JSON)
        } When {
            post("/api/businesses")
        } Then {
            statusCode(400)
        }
    }

    @Test
    fun `search businesses without filters returns page`() {
        Given {
            queryParam("size", 20)
        } When {
            get("/api/businesses")
        } Then {
            statusCode(200)
            body("content.size()", greaterThan(0))
            body("totalElements", greaterThan(0))
            body("content[0].id", notNullValue())
            body("content[0].name", equalTo("Test Business"))
        }
    }

    @Test
    fun `search businesses with type filter returns filtered`() {
        Given {
            queryParam("typeCode", businessTypeCode)
            queryParam("size", 20)
        } When {
            get("/api/businesses")
        } Then {
            statusCode(200)
            body("totalElements", greaterThan(0))
            body("content.every { it.typeCode == '$businessTypeCode' }", equalTo(true))
        }
    }

    @Test
    fun `search businesses with municipality filter returns filtered`() {
        Given {
            queryParam("municipalityCode", municipalityCode)
            queryParam("size", 20)
        } When {
            get("/api/businesses")
        } Then {
            statusCode(200)
            body("totalElements", greaterThan(0))
            body("content.every { it.municipalityCode == '$municipalityCode' }", equalTo(true))
        }
    }

    @Test
    fun `search businesses with both filters returns filtered`() {
        Given {
            queryParam("typeCode", businessTypeCode)
            queryParam("municipalityCode", municipalityCode)
            queryParam("size", 20)
        } When {
            get("/api/businesses")
        } Then {
            statusCode(200)
            body("totalElements", greaterThan(0))
            body(
                "content.every { it.typeCode == '$businessTypeCode' && it.municipalityCode == '$municipalityCode' }",
                equalTo(true)
            )
        }
    }

    @Test
    fun `get business by id returns business details`() {
        When {
            get("/api/businesses/$businessId")
        } Then {
            statusCode(200)
            body("id", equalTo(businessId.toInt()))
            body("name", equalTo("Test Business"))
            body("typeCode", equalTo(businessTypeCode))
            body("municipalityCode", equalTo(municipalityCode))
            body("address", equalTo("123 Main St"))
            body("phone", equalTo("123456789"))
            body("email", equalTo("business@test.com"))
            body("description", equalTo("A test business"))
            body("ownerUsername", equalTo(ownerUsername))
        }
    }

    @Test
    fun `get non-existent business returns 404`() {
        When {
            get("/api/businesses/99999")
        } Then {
            statusCode(404)
        }
    }

    @Test
    fun `update business with valid data returns updated business`() {
        val updatePayload = """
        {
            "name": "Updated Business",
            "typeCode": "$businessTypeCode",
            "municipalityCode": "$municipalityCode",
            "address": "789 New St",
            "phone": "555555555",
            "email": "updated@business.com",
            "description": "Updated description"
        }
        """.trimIndent()

        Given {
            header("Authorization", "Bearer $ownerToken")
            body(updatePayload)
            contentType(ContentType.JSON)
        } When {
            put("/api/businesses/$businessId")
        } Then {
            statusCode(200)
            body("id", equalTo(businessId.toInt()))
            body("name", equalTo("Updated Business"))
            body("address", equalTo("789 New St"))
            body("phone", equalTo("555555555"))
            body("email", equalTo("updated@business.com"))
            body("description", equalTo("Updated description"))
        }
    }

    @Test
    fun `update business with invalid data returns 400`() {
        val invalidPayload = """
        {
            "name": ""
        }
        """.trimIndent()

        Given {
            header("Authorization", "Bearer $ownerToken")
            body(invalidPayload)
            contentType(ContentType.JSON)
        } When {
            put("/api/businesses/$businessId")
        } Then {
            statusCode(400)
        }
    }

    @Test
    fun `update business by non-owner returns 403`() {
        val otherUsername = "otherbiz_${System.currentTimeMillis()}"
        Given {
            body(
                """
                {
                    "username": "$otherUsername",
                    "email": "$otherUsername@test.com",
                    "password": "Password1!",
                    "firstName": "Other",
                    "lastName": "User"
                }
                """.trimIndent()
            )
            contentType(ContentType.JSON)
        } When {
            post("/api/auth/register")
        } Then {
            statusCode(201)
        }

        val otherToken = Given {
            body("""{"username":"$otherUsername","password":"Password1!"}""")
            contentType(ContentType.JSON)
        } When {
            post("/api/auth/login")
        } Then {
            statusCode(200)
        } Extract {
            jsonPath().getString("accessToken")
        }

        val updatePayload = """
        {
            "name": "Hacked Business",
            "typeCode": "$businessTypeCode",
            "municipalityCode": "$municipalityCode"
        }
        """.trimIndent()

        Given {
            header("Authorization", "Bearer $otherToken")
            body(updatePayload)
            contentType(ContentType.JSON)
        } When {
            put("/api/businesses/$businessId")
        } Then {
            statusCode(403)
            body("detail", equalTo("You do not own this business"))
        }
    }

    @Test
    fun `delete business by owner returns 204`() {
        Given {
            header("Authorization", "Bearer $ownerToken")
        } When {
            delete("/api/businesses/$businessId")
        } Then {
            statusCode(204)
        }

        When {
            get("/api/businesses/$businessId")
        } Then {
            statusCode(404)
        }
    }

    @Test
    fun `delete business by non-owner returns 403`() {
        val otherUsername = "deleter_${System.currentTimeMillis()}"
        Given {
            body(
                """
                {
                    "username": "$otherUsername",
                    "email": "$otherUsername@test.com",
                    "password": "Password1!",
                    "firstName": "Deleter",
                    "lastName": "User"
                }
                """.trimIndent()
            )
            contentType(ContentType.JSON)
        } When {
            post("/api/auth/register")
        } Then {
            statusCode(201)
        }

        val otherToken = Given {
            body("""{"username":"$otherUsername","password":"Password1!"}""")
            contentType(ContentType.JSON)
        } When {
            post("/api/auth/login")
        } Then {
            statusCode(200)
        } Extract {
            jsonPath().getString("accessToken")
        }

        Given {
            header("Authorization", "Bearer $otherToken")
        } When {
            delete("/api/businesses/$businessId")
        } Then {
            statusCode(403)
            body("detail", equalTo("You do not own this business"))
        }

        When {
            get("/api/businesses/$businessId")
        } Then {
            statusCode(200)
            body("id", equalTo(businessId.toInt()))
        }
    }

    @Test
    fun `delete non-existent business returns 404`() {
        Given {
            header("Authorization", "Bearer $ownerToken")
        } When {
            delete("/api/businesses/99999")
        } Then {
            statusCode(404)
        }
    }
}