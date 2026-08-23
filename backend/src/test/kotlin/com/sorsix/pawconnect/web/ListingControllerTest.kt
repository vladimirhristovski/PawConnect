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
import javax.sql.DataSource

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class ListingControllerTest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var dataSource: DataSource

    @MockitoBean
    private lateinit var emailService: EmailService

    private lateinit var ownerToken: String
    private lateinit var ownerUsername: String
    private var businessId: Long = 0L
    private lateinit var speciesCode: String
    private lateinit var speciesName: String
    private lateinit var municipalityCode: String
    private lateinit var businessTypeCode: String

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
                        refresh_tokens,
                        password_reset_tokens,
                        user_roles,
                        users,
                        adoption_applications,
                        businesses,
                        listings,
                        pet_photos,
                        pets
                    RESTART IDENTITY CASCADE
                    """.trimIndent()
                )
                statement.execute("SET session_replication_role = 'origin'")
            }
        }
    }

    private fun prepareLookupCodes() {
        val speciesResponse = Given {
            accept(ContentType.JSON)
        } When {
            get("/api/lookups/species")
        } Then {
            statusCode(200)
        } Extract {
            jsonPath()
        }
        speciesCode = speciesResponse.getString("[0].code")
        speciesName = speciesResponse.getString("[0].name")

        municipalityCode = Given {
            accept(ContentType.JSON)
        } When {
            get("/api/lookups/municipalities")
        } Then {
            statusCode(200)
        } Extract {
            jsonPath().getString("[0].code")
        }

        businessTypeCode = Given {
            accept(ContentType.JSON)
        } When {
            get("/api/lookups/business-types")
        } Then {
            statusCode(200)
        } Extract {
            jsonPath().getString("[0].code")
        }
    }

    private fun prepareOwnerAndBusiness() {
        val username = "listingowner_${System.currentTimeMillis()}"
        ownerUsername = username
        val email = "$username@test.com"

        Given {
            body(
                """
                {
                    "username": "$username",
                    "email": "$email",
                    "password": "Password1!",
                    "firstName": "Listing",
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

        val createBusinessPayload = """
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
            body(createBusinessPayload)
            contentType(ContentType.JSON)
        } When {
            post("/api/businesses")
        } Then {
            statusCode(201)
        } Extract {
            jsonPath().getLong("id")
        }
    }

    private fun createListing(
        token: String,
        petName: String = "TestPet",
        draft: Boolean = true,
        municipality: String = municipalityCode,
        useBusiness: Boolean = false
    ): Long {
        val businessField = if (useBusiness) """, "businessId": $businessId""" else ""
        val payload = """
        {
            "pet": {
                "name": "$petName",
                "speciesCode": "$speciesCode",
                "breedCodes": [],
                "gender": "MALE",
                "size": "MEDIUM"
            },
            "municipalityCode": "$municipality",
            "saveAsDraft": $draft
            $businessField
        }
        """.trimIndent()

        return Given {
            header("Authorization", "Bearer $token")
            body(payload)
            contentType(ContentType.JSON)
        } When {
            post("/api/listings")
        } Then {
            statusCode(201)
        } Extract {
            jsonPath().getLong("id")
        }
    }

    private fun registerAndLogin(usernamePrefix: String): String {
        val username = "${usernamePrefix}_${System.currentTimeMillis()}"
        val email = "$username@test.com"

        Given {
            body(
                """
                {
                    "username": "$username",
                    "email": "$email",
                    "password": "Password1!",
                    "firstName": "Test",
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
        return loginResponse.getString("accessToken")
    }


    @Test
    fun `create listing with new pet as draft returns 201`() {
        val payload = """
        {
            "pet": {
                "name": "Buddy",
                "speciesCode": "$speciesCode",
                "breedCodes": [],
                "gender": "MALE",
                "size": "MEDIUM",
                "description": "Friendly dog"
            },
            "municipalityCode": "$municipalityCode",
            "title": "Buddy looking for home",
            "adoptionFee": 50.00,
            "saveAsDraft": true
        }
        """.trimIndent()

        Given {
            header("Authorization", "Bearer $ownerToken")
            body(payload)
            contentType(ContentType.JSON)
        } When {
            post("/api/listings")
        } Then {
            statusCode(201)
            body("id", notNullValue())
            body("pet.name", equalTo("Buddy"))
            body("statusCode", equalTo("DRAFT"))
            body("postedBy", equalTo(ownerUsername))
            body("municipalityCode", equalTo(municipalityCode))
            body("title", equalTo("Buddy looking for home"))
            body("adoptionFee", equalTo(50.0f))
        }
    }

    @Test
    fun `create listing with existing pet returns 201`() {
        val firstListingId = createListing(ownerToken, "ExistingPet", draft = true)
        val petId = Given {
            header("Authorization", "Bearer $ownerToken")
        } When {
            get("/api/listings/$firstListingId")
        } Then {
            statusCode(200)
        } Extract {
            jsonPath().getLong("pet.id")
        }

        Given {
            header("Authorization", "Bearer $ownerToken")
        } When {
            delete("/api/listings/$firstListingId")
        } Then {
            statusCode(204)
        }

        val payload = """
        {
            "petId": $petId,
            "municipalityCode": "$municipalityCode",
            "title": "Second listing for same pet",
            "adoptionFee": 30.00,
            "saveAsDraft": true
        }
        """.trimIndent()

        Given {
            header("Authorization", "Bearer $ownerToken")
            body(payload)
            contentType(ContentType.JSON)
        } When {
            post("/api/listings")
        } Then {
            statusCode(201)
            body("id", notNullValue())
            body("pet.id", equalTo(petId.toInt()))
        }
    }

    @Test
    fun `create listing with business association returns 201`() {
        val payload = """
        {
            "pet": {
                "name": "BizPet",
                "speciesCode": "$speciesCode",
                "breedCodes": [],
                "gender": "MALE",
                "size": "LARGE"
            },
            "businessId": $businessId,
            "municipalityCode": "$municipalityCode",
            "title": "Business listing",
            "saveAsDraft": true
        }
        """.trimIndent()

        Given {
            header("Authorization", "Bearer $ownerToken")
            body(payload)
            contentType(ContentType.JSON)
        } When {
            post("/api/listings")
        } Then {
            statusCode(201)
            body("business.id", equalTo(businessId.toInt()))
            body("business.name", equalTo("Test Business"))
        }
    }

    @Test
    fun `create listing with both pet and petId returns 400`() {
        val payload = """
        {
            "petId": 1,
            "pet": {
                "name": "Invalid",
                "speciesCode": "$speciesCode",
                "breedCodes": [],
                "gender": "MALE",
                "size": "MEDIUM"
            },
            "municipalityCode": "$municipalityCode"
        }
        """.trimIndent()

        Given {
            header("Authorization", "Bearer $ownerToken")
            body(payload)
            contentType(ContentType.JSON)
        } When {
            post("/api/listings")
        } Then {
            statusCode(400)
            body("detail", containsString("Provide either petId or pet, not both"))
        }
    }

    @Test
    fun `create listing with neither pet nor petId returns 400`() {
        val payload = """
        {
            "municipalityCode": "$municipalityCode"
        }
        """.trimIndent()

        Given {
            header("Authorization", "Bearer $ownerToken")
            body(payload)
            contentType(ContentType.JSON)
        } When {
            post("/api/listings")
        } Then {
            statusCode(400)
            body("detail", containsString("Either petId or pet must be provided"))
        }
    }

    @Test
    fun `create listing with invalid municipality returns 404`() {
        val payload = """
        {
            "pet": {
                "name": "Invalid",
                "speciesCode": "$speciesCode",
                "breedCodes": [],
                "gender": "MALE",
                "size": "MEDIUM"
            },
            "municipalityCode": "INVALID"
        }
        """.trimIndent()

        Given {
            header("Authorization", "Bearer $ownerToken")
            body(payload)
            contentType(ContentType.JSON)
        } When {
            post("/api/listings")
        } Then {
            statusCode(404)
            body("detail", containsString("Municipality not found"))
        }
    }

    @Test
    fun `create listing with existing open listing for same pet returns 409`() {
        val firstListingId = createListing(ownerToken, "ConflictPet", draft = true)
        Given {
            header("Authorization", "Bearer $ownerToken")
        } When {
            post("/api/listings/$firstListingId/publish")
        } Then {
            statusCode(200)
        }

        val petId = Given {
            header("Authorization", "Bearer $ownerToken")
        } When {
            get("/api/listings/$firstListingId")
        } Then {
            statusCode(200)
        } Extract {
            jsonPath().getLong("pet.id")
        }

        val payload = """
        {
            "petId": $petId,
            "municipalityCode": "$municipalityCode",
            "title": "Duplicate listing",
            "saveAsDraft": true
        }
        """.trimIndent()

        Given {
            header("Authorization", "Bearer $ownerToken")
            body(payload)
            contentType(ContentType.JSON)
        } When {
            post("/api/listings")
        } Then {
            statusCode(409)
            body("detail", containsString("already has an open listing"))
        }
    }

    @Test
    fun `get listing as owner sees draft`() {
        val listingId = createListing(ownerToken, draft = true)

        Given {
            header("Authorization", "Bearer $ownerToken")
        } When {
            get("/api/listings/$listingId")
        } Then {
            statusCode(200)
            body("id", equalTo(listingId.toInt()))
            body("statusCode", equalTo("DRAFT"))
        }
    }

    @Test
    fun `get listing without token for draft returns 404`() {
        val listingId = createListing(ownerToken, draft = true)

        When {
            get("/api/listings/$listingId")
        } Then {
            statusCode(404)
            body("detail", equalTo("Listing not found"))
        }
    }

    @Test
    fun `get listing as non-owner for draft returns 404`() {
        val otherToken = registerAndLogin("otheruser")
        val listingId = createListing(ownerToken, draft = true)

        Given {
            header("Authorization", "Bearer $otherToken")
        } When {
            get("/api/listings/$listingId")
        } Then {
            statusCode(404)
            body("detail", equalTo("Listing not found"))
        }
    }

    @Test
    fun `get listing without token for active returns 200`() {
        val listingId = createListing(ownerToken, draft = true)
        Given {
            header("Authorization", "Bearer $ownerToken")
        } When {
            post("/api/listings/$listingId/publish")
        } Then {
            statusCode(200)
        }

        When {
            get("/api/listings/$listingId")
        } Then {
            statusCode(200)
            body("statusCode", equalTo("ACTIVE"))
        }
    }

    @Test
    fun `get listing that does not exist returns 404`() {
        When {
            get("/api/listings/99999")
        } Then {
            statusCode(404)
        }
    }

    @Test
    fun `update listing as owner while draft succeeds`() {
        val listingId = createListing(ownerToken, draft = true)

        val updatePayload = """
        {
            "title": "Updated Title",
            "description": "New description",
            "adoptionFee": 75.00
        }
        """.trimIndent()

        Given {
            header("Authorization", "Bearer $ownerToken")
            body(updatePayload)
            contentType(ContentType.JSON)
        } When {
            put("/api/listings/$listingId")
        } Then {
            statusCode(200)
            body("title", equalTo("Updated Title"))
            body("description", equalTo("New description"))
            body("adoptionFee", equalTo(75.0f))
        }
    }

    @Test
    fun `update listing as owner while active succeeds`() {
        val listingId = createListing(ownerToken, draft = true)
        Given {
            header("Authorization", "Bearer $ownerToken")
        } When {
            post("/api/listings/$listingId/publish")
        } Then {
            statusCode(200)
        }

        val updatePayload = """
        {
            "title": "Active Updated"
        }
        """.trimIndent()

        Given {
            header("Authorization", "Bearer $ownerToken")
            body(updatePayload)
            contentType(ContentType.JSON)
        } When {
            put("/api/listings/$listingId")
        } Then {
            statusCode(200)
            body("title", equalTo("Active Updated"))
        }
    }

    @Test
    fun `update listing as non-owner returns 403`() {
        val listingId = createListing(ownerToken, draft = true)
        val otherToken = registerAndLogin("otheruser2")

        val updatePayload = """{"title": "Hacked"}"""

        Given {
            header("Authorization", "Bearer $otherToken")
            body(updatePayload)
            contentType(ContentType.JSON)
        } When {
            put("/api/listings/$listingId")
        } Then {
            statusCode(403)
            body("detail", equalTo("You do not own this listing"))
        }
    }

    @Test
    fun `update listing with cancelled status returns 409`() {
        val listingId = createListing(ownerToken, draft = true)
        Given {
            header("Authorization", "Bearer $ownerToken")
        } When {
            post("/api/listings/$listingId/publish")
        } Then {
            statusCode(200)
        }
        Given {
            header("Authorization", "Bearer $ownerToken")
        } When {
            post("/api/listings/$listingId/cancel")
        } Then {
            statusCode(200)
        }

        val updatePayload = """{"title": "Should fail"}"""

        Given {
            header("Authorization", "Bearer $ownerToken")
            body(updatePayload)
            contentType(ContentType.JSON)
        } When {
            put("/api/listings/$listingId")
        } Then {
            statusCode(409)
            body("detail", containsString("cannot be updated"))
        }
    }

    @Test
    fun `publish draft listing returns 200`() {
        val listingId = createListing(ownerToken, draft = true)

        Given {
            header("Authorization", "Bearer $ownerToken")
        } When {
            post("/api/listings/$listingId/publish")
        } Then {
            statusCode(200)
            body("statusCode", equalTo("ACTIVE"))
        }
    }

    @Test
    fun `publish already active listing returns 409`() {
        val listingId = createListing(ownerToken, draft = true)
        Given {
            header("Authorization", "Bearer $ownerToken")
        } When {
            post("/api/listings/$listingId/publish")
        } Then {
            statusCode(200)
        }

        Given {
            header("Authorization", "Bearer $ownerToken")
        } When {
            post("/api/listings/$listingId/publish")
        } Then {
            statusCode(409)
            body("detail", containsString("not in DRAFT state"))
        }
    }

    @Test
    fun `publish cancelled listing returns 409`() {
        val listingId = createListing(ownerToken, draft = true)
        Given {
            header("Authorization", "Bearer $ownerToken")
        } When {
            post("/api/listings/$listingId/publish")
        } Then {
            statusCode(200)
        }
        Given {
            header("Authorization", "Bearer $ownerToken")
        } When {
            post("/api/listings/$listingId/cancel")
        } Then {
            statusCode(200)
        }

        Given {
            header("Authorization", "Bearer $ownerToken")
        } When {
            post("/api/listings/$listingId/publish")
        } Then {
            statusCode(409)
            body("detail", containsString("not in DRAFT state"))
        }
    }

    @Test
    fun `cancel active listing returns 200`() {
        val listingId = createListing(ownerToken, draft = true)
        Given {
            header("Authorization", "Bearer $ownerToken")
        } When {
            post("/api/listings/$listingId/publish")
        } Then {
            statusCode(200)
        }

        Given {
            header("Authorization", "Bearer $ownerToken")
        } When {
            post("/api/listings/$listingId/cancel")
        } Then {
            statusCode(200)
            body("statusCode", equalTo("CANCELLED"))
        }

        When {
            get("/api/listings/$listingId")
        } Then {
            statusCode(404)
        }
    }

    @Test
    fun `cancel draft listing returns 200`() {
        val listingId = createListing(ownerToken, draft = true)

        Given {
            header("Authorization", "Bearer $ownerToken")
        } When {
            post("/api/listings/$listingId/cancel")
        } Then {
            statusCode(200)
            body("statusCode", equalTo("CANCELLED"))
        }
    }

    @Test
    fun `cancel already cancelled listing returns 409`() {
        val listingId = createListing(ownerToken, draft = true)
        Given {
            header("Authorization", "Bearer $ownerToken")
        } When {
            post("/api/listings/$listingId/cancel")
        } Then {
            statusCode(200)
        }

        Given {
            header("Authorization", "Bearer $ownerToken")
        } When {
            post("/api/listings/$listingId/cancel")
        } Then {
            statusCode(409)
            body("detail", containsString("already adopted or cancelled"))
        }
    }

    @Test
    fun `delete listing as owner returns 204`() {
        val listingId = createListing(ownerToken, draft = true)

        Given {
            header("Authorization", "Bearer $ownerToken")
        } When {
            delete("/api/listings/$listingId")
        } Then {
            statusCode(204)
        }

        Given {
            header("Authorization", "Bearer $ownerToken")
        } When {
            get("/api/listings/$listingId")
        } Then {
            statusCode(404)
        }
    }

    @Test
    fun `delete listing as non-owner returns 403`() {
        val listingId = createListing(ownerToken, draft = true)
        val otherToken = registerAndLogin("otheruser3")

        Given {
            header("Authorization", "Bearer $otherToken")
        } When {
            delete("/api/listings/$listingId")
        } Then {
            statusCode(403)
            body("detail", equalTo("You do not own this listing"))
        }
    }

    @Test
    fun `delete non-existent listing returns 404`() {
        Given {
            header("Authorization", "Bearer $ownerToken")
        } When {
            delete("/api/listings/99999")
        } Then {
            statusCode(404)
        }
    }

    @Test
    fun `search listings with species filter returns expected`() {
        val listingId = createListing(ownerToken, petName = "SearchDog", draft = false)

        Given {
            queryParam("speciesCode", speciesCode)
            queryParam("size", 20)
        } When {
            get("/api/listings")
        } Then {
            statusCode(200)
            body("totalElements", greaterThan(0))
            body("content.find { it.id == $listingId }.pet.speciesName", equalTo(speciesName))
        }
    }

    @Test
    fun `search listings with municipality filter returns expected`() {
        val listingId = createListing(ownerToken, municipality = municipalityCode, draft = false)

        Given {
            queryParam("municipalityCode", municipalityCode)
            queryParam("size", 20)
        } When {
            get("/api/listings")
        } Then {
            statusCode(200)
            body("totalElements", greaterThan(0))
            body("content.find { it.id == $listingId }.municipalityName", notNullValue())
        }
    }

    @Test
    fun `search listings with gender and size filters`() {
        val listingId = createListing(ownerToken, petName = "FilteredPet", draft = false)

        Given {
            queryParam("gender", "MALE")
            queryParam("petSize", "MEDIUM")
            queryParam("size", 20)
        } When {
            get("/api/listings")
        } Then {
            statusCode(200)
            body("totalElements", greaterThan(0))
        }
    }

    @Test
    fun `list my listings returns only my listings`() {
        val id1 = createListing(ownerToken, petName = "Mine1", draft = true)
        val id2 = createListing(ownerToken, petName = "Mine2", draft = true)

        val otherToken = registerAndLogin("otheruser4")
        val otherId = createListing(otherToken, petName = "Other", draft = true)

        Given {
            header("Authorization", "Bearer $ownerToken")
            queryParam("size", 20)
        } When {
            get("/api/listings/mine")
        } Then {
            statusCode(200)
            body("totalElements", equalTo(2))
            body("content*.id", containsInAnyOrder(id1.toInt(), id2.toInt()))
            body("content*.id", not(hasItem(otherId.toInt())))
        }
    }

    @Test
    fun `list my listings excludes deleted listings`() {
        val listingId = createListing(ownerToken, draft = true)
        Given {
            header("Authorization", "Bearer $ownerToken")
        } When {
            delete("/api/listings/$listingId")
        } Then {
            statusCode(204)
        }

        Given {
            header("Authorization", "Bearer $ownerToken")
            queryParam("size", 20)
        } When {
            get("/api/listings/mine")
        } Then {
            statusCode(200)
            body("totalElements", equalTo(0))
        }
    }

    @Test
    fun `create another listing for same pet after soft-delete succeeds`() {
        val listingId = createListing(ownerToken, "DeletedPet", draft = true)
        val petId = Given {
            header("Authorization", "Bearer $ownerToken")
        } When {
            get("/api/listings/$listingId")
        } Then {
            statusCode(200)
        } Extract {
            jsonPath().getLong("pet.id")
        }

        Given {
            header("Authorization", "Bearer $ownerToken")
        } When {
            delete("/api/listings/$listingId")
        } Then {
            statusCode(204)
        }

        val payload = """
        {
            "petId": $petId,
            "municipalityCode": "$municipalityCode",
            "title": "New listing after delete",
            "saveAsDraft": true
        }
        """.trimIndent()

        Given {
            header("Authorization", "Bearer $ownerToken")
            body(payload)
            contentType(ContentType.JSON)
        } When {
            post("/api/listings")
        } Then {
            statusCode(201)
            body("id", notNullValue())
            body("pet.id", equalTo(petId.toInt()))
        }
    }

    @Test
    fun `search listings with minFee and maxFee filters`() {
        val listingId = createListing(ownerToken, draft = false)

        val updatePayload = """{"adoptionFee": 100.00}"""
        Given {
            header("Authorization", "Bearer $ownerToken")
            body(updatePayload)
            contentType(ContentType.JSON)
        } When {
            put("/api/listings/$listingId")
        } Then {
            statusCode(200)
        }

        Given {
            queryParam("minFee", 50)
            queryParam("maxFee", 150)
            queryParam("size", 20)
        } When {
            get("/api/listings")
        } Then {
            statusCode(200)
            body("totalElements", greaterThan(0))
            body("content.find { it.id == $listingId }.adoptionFee", equalTo(100.0f))
        }
    }
}