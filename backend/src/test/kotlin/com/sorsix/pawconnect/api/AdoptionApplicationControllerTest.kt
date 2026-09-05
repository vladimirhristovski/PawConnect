package com.sorsix.pawconnect.api

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
class AdoptionApplicationControllerTest {
    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var dataSource: DataSource

    @MockitoBean
    private lateinit var emailService: EmailService

    private lateinit var ownerToken: String
    private lateinit var ownerUsername: String
    private lateinit var applicantToken: String
    private lateinit var applicantUsername: String
    private lateinit var speciesCode: String
    private lateinit var municipalityCode: String

    @BeforeEach
    fun setup() {
        RestAssured.port = port
        cleanDatabase()
        prepareLookupCodes()
        prepareUsersAndTokens()
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
                    """.trimIndent(),
                )
                statement.execute("SET session_replication_role = 'origin'")
            }
        }
    }

    private fun prepareLookupCodes() {
        speciesCode = Given {
            accept(ContentType.JSON)
        } When {
            get("/api/lookups/species")
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

    private fun prepareUsersAndTokens() {
        val ownerTimestamp = System.currentTimeMillis()
        ownerUsername = "owner_$ownerTimestamp"
        val ownerEmail = "$ownerUsername@test.com"
        registerUser(ownerUsername, ownerEmail)

        val ownerLogin = login(ownerUsername)
        ownerToken = ownerLogin.getString("accessToken")

        val applicantTimestamp = System.currentTimeMillis() + 1
        applicantUsername = "applicant_$applicantTimestamp"
        val applicantEmail = "$applicantUsername@test.com"
        registerUser(applicantUsername, applicantEmail)

        val applicantLogin = login(applicantUsername)
        applicantToken = applicantLogin.getString("accessToken")
    }

    private fun registerUser(
        username: String,
        email: String,
    ) {
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
                """.trimIndent(),
            )
            contentType(ContentType.JSON)
        } When {
            post("/api/auth/register")
        } Then {
            statusCode(201)
        }
    }

    private fun login(username: String): io.restassured.path.json.JsonPath =
        Given {
            body("""{"username":"$username","password":"Password1!"}""")
            contentType(ContentType.JSON)
        } When {
            post("/api/auth/login")
        } Then {
            statusCode(200)
        } Extract {
            jsonPath()
        }

    private fun createActiveListing(
        token: String,
        petName: String = "TestPet",
    ): Long {
        val payload =
            """
            {
                "pet": {
                    "name": "$petName",
                    "speciesCode": "$speciesCode",
                    "breedCodes": [],
                    "gender": "MALE",
                    "size": "MEDIUM",
                    "description": "Test pet"
                },
                "municipalityCode": "$municipalityCode",
                "title": "Active Listing",
                "adoptionFee": 50,
                "saveAsDraft": false
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

    private fun createDraftListing(
        token: String,
        petName: String = "DraftPet",
    ): Long {
        val payload =
            """
            {
                "pet": {
                    "name": "$petName",
                    "speciesCode": "$speciesCode",
                    "breedCodes": [],
                    "gender": "MALE",
                    "size": "MEDIUM"
                },
                "municipalityCode": "$municipalityCode",
                "title": "Draft Listing",
                "saveAsDraft": true
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

    private fun submitApplication(
        token: String,
        listingId: Long,
        message: String = "I want this pet",
    ): Long {
        val payload =
            """
            {
                "message": "$message",
                "contactPhone": "123456789",
                "contactEmail": "applicant@test.com"
            }
            """.trimIndent()

        return Given {
            header("Authorization", "Bearer $token")
            body(payload)
            contentType(ContentType.JSON)
        } When {
            post("/api/listings/$listingId/applications")
        } Then {
            statusCode(201)
        } Extract {
            jsonPath().getLong("id")
        }
    }

    @Test
    fun `submit application to active listing returns 201`() {
        val listingId = createActiveListing(ownerToken)
        val appId = submitApplication(applicantToken, listingId)

        Given {
            header("Authorization", "Bearer $applicantToken")
        } When {
            get("/api/applications/mine")
        } Then {
            statusCode(200)
            body("content.size()", equalTo(1))
            body("content[0].id", equalTo(appId.toInt()))
            body("content[0].listingId", equalTo(listingId.toInt()))
            body("content[0].statusCode", equalTo("SUBMITTED"))
        }
    }

    @Test
    fun `submit application to inactive listing returns 409`() {
        val draftListingId = createDraftListing(ownerToken)

        Given {
            header("Authorization", "Bearer $applicantToken")
            body("""{"message":"Apply"}""")
            contentType(ContentType.JSON)
        } When {
            post("/api/listings/$draftListingId/applications")
        } Then {
            statusCode(409)
            body("detail", containsString("Listing is not currently accepting applications"))
        }
    }

    @Test
    fun `submit application to own listing returns 403`() {
        val listingId = createActiveListing(ownerToken)

        Given {
            header("Authorization", "Bearer $ownerToken")
            body("""{"message":"Apply"}""")
            contentType(ContentType.JSON)
        } When {
            post("/api/listings/$listingId/applications")
        } Then {
            statusCode(403)
            body("detail", containsString("You cannot apply to your own listing"))
        }
    }

    @Test
    fun `submit duplicate pending application returns 409`() {
        val listingId = createActiveListing(ownerToken)
        submitApplication(applicantToken, listingId)

        Given {
            header("Authorization", "Bearer $applicantToken")
            body("""{"message":"Another"}""")
            contentType(ContentType.JSON)
        } When {
            post("/api/listings/$listingId/applications")
        } Then {
            statusCode(409)
            body("detail", containsString("already have a pending application"))
        }
    }

    @Test
    fun `submit application to non-existent listing returns 404`() {
        Given {
            header("Authorization", "Bearer $applicantToken")
            body("""{"message":"Apply"}""")
            contentType(ContentType.JSON)
        } When {
            post("/api/listings/99999/applications")
        } Then {
            statusCode(404)
            body("detail", containsString("Listing not found"))
        }
    }

    @Test
    fun `submit application with invalid request returns 400`() {
        val listingId = createActiveListing(ownerToken)
        Given {
            header("Authorization", "Bearer $applicantToken")
            body("{invalid}")
            contentType(ContentType.JSON)
        } When {
            post("/api/listings/$listingId/applications")
        } Then {
            statusCode(400)
        }
    }

    @Test
    fun `list applications for listing as owner returns 200`() {
        val listingId = createActiveListing(ownerToken)
        submitApplication(applicantToken, listingId)

        Given {
            header("Authorization", "Bearer $ownerToken")
        } When {
            get("/api/listings/$listingId/applications")
        } Then {
            statusCode(200)
            body("totalElements", equalTo(1))
            body("content[0].applicantUsername", equalTo(applicantUsername))
        }
    }

    @Test
    fun `list applications for listing as non-owner returns 403`() {
        val listingId = createActiveListing(ownerToken)
        submitApplication(applicantToken, listingId)

        val otherUsername = "other_${System.currentTimeMillis()}"
        registerUser(otherUsername, "$otherUsername@test.com")
        val otherToken = login(otherUsername).getString("accessToken")

        Given {
            header("Authorization", "Bearer $otherToken")
        } When {
            get("/api/listings/$listingId/applications")
        } Then {
            statusCode(403)
            body("detail", containsString("not authorized to view applications"))
        }
    }

    @Test
    fun `list applications for non-existent listing returns 404`() {
        Given {
            header("Authorization", "Bearer $ownerToken")
        } When {
            get("/api/listings/99999/applications")
        } Then {
            statusCode(404)
        }
    }

    @Test
    fun `list my applications returns only mine`() {
        val listing1 = createActiveListing(ownerToken)
        val listing2 = createActiveListing(ownerToken)
        submitApplication(applicantToken, listing1)
        submitApplication(applicantToken, listing2)

        Given {
            header("Authorization", "Bearer $applicantToken")
        } When {
            get("/api/applications/mine")
        } Then {
            statusCode(200)
            body("totalElements", equalTo(2))
            body("content*.listingId", containsInAnyOrder(listing1.toInt(), listing2.toInt()))
        }

        val otherUsername = "other2_${System.currentTimeMillis()}"
        registerUser(otherUsername, "$otherUsername@test.com")
        val otherToken = login(otherUsername).getString("accessToken")
        Given {
            header("Authorization", "Bearer $otherToken")
        } When {
            get("/api/applications/mine")
        } Then {
            statusCode(200)
            body("totalElements", equalTo(0))
        }
    }

    @Test
    fun `review application as owner approve returns 200 and rejects others`() {
        val listingId = createActiveListing(ownerToken)
        val appId1 = submitApplication(applicantToken, listingId)

        val applicant2Username = "applicant2_${System.currentTimeMillis()}"
        registerUser(applicant2Username, "$applicant2Username@test.com")
        val applicant2Token = login(applicant2Username).getString("accessToken")
        val appId2 = submitApplication(applicant2Token, listingId, "I also want it")

        Given {
            header("Authorization", "Bearer $ownerToken")
            queryParam("decision", "APPROVE")
        } When {
            patch("/api/applications/$appId1/review")
        } Then {
            statusCode(200)
            body("id", equalTo(appId1.toInt()))
            body("statusCode", equalTo("APPROVED"))
        }

        Given {
            header("Authorization", "Bearer $ownerToken")
        } When {
            get("/api/listings/$listingId/applications")
        } Then {
            statusCode(200)
            body("content.find { it.id == $appId1 }.statusCode", equalTo("APPROVED"))
            body("content.find { it.id == $appId2 }.statusCode", equalTo("REJECTED"))
        }

        Given {
            header("Authorization", "Bearer $ownerToken")
        } When {
            get("/api/listings/$listingId")
        } Then {
            statusCode(200)
            body("statusCode", not(equalTo("ACTIVE")))
        }
    }

    @Test
    fun `review application as owner reject returns 200`() {
        val listingId = createActiveListing(ownerToken)
        val appId = submitApplication(applicantToken, listingId)

        Given {
            header("Authorization", "Bearer $ownerToken")
            queryParam("decision", "REJECT")
        } When {
            patch("/api/applications/$appId/review")
        } Then {
            statusCode(200)
            body("statusCode", equalTo("REJECTED"))
        }

        Given {
            header("Authorization", "Bearer $ownerToken")
        } When {
            get("/api/listings/$listingId")
        } Then {
            statusCode(200)
            body("statusCode", equalTo("ACTIVE"))
        }
    }

    @Test
    fun `review application as non-owner returns 403`() {
        val listingId = createActiveListing(ownerToken)
        val appId = submitApplication(applicantToken, listingId)

        Given {
            header("Authorization", "Bearer $applicantToken")
            queryParam("decision", "APPROVE")
        } When {
            patch("/api/applications/$appId/review")
        } Then {
            statusCode(403)
            body("detail", containsString("not authorized to review"))
        }
    }

    @Test
    fun `review non-existent application returns 404`() {
        Given {
            header("Authorization", "Bearer $ownerToken")
            queryParam("decision", "APPROVE")
        } When {
            patch("/api/applications/99999/review")
        } Then {
            statusCode(404)
        }
    }

    @Test
    fun `review already reviewed application returns 409`() {
        val listingId = createActiveListing(ownerToken)
        val appId = submitApplication(applicantToken, listingId)

        Given {
            header("Authorization", "Bearer $ownerToken")
            queryParam("decision", "APPROVE")
        } When {
            patch("/api/applications/$appId/review")
        } Then {
            statusCode(200)
        }

        Given {
            header("Authorization", "Bearer $ownerToken")
            queryParam("decision", "APPROVE")
        } When {
            patch("/api/applications/$appId/review")
        } Then {
            statusCode(409)
            body("detail", containsString("not in a pending state"))
        }
    }

    @Test
    fun `withdraw application as applicant returns 200`() {
        val listingId = createActiveListing(ownerToken)
        val appId = submitApplication(applicantToken, listingId)

        Given {
            header("Authorization", "Bearer $applicantToken")
        } When {
            post("/api/applications/$appId/withdraw")
        } Then {
            statusCode(200)
            body("statusCode", equalTo("WITHDRAWN"))
        }

        Given {
            header("Authorization", "Bearer $ownerToken")
        } When {
            get("/api/listings/$listingId")
        } Then {
            statusCode(200)
            body("statusCode", equalTo("ACTIVE"))
        }
    }

    @Test
    fun `withdraw application as non-applicant returns 403`() {
        val listingId = createActiveListing(ownerToken)
        val appId = submitApplication(applicantToken, listingId)

        Given {
            header("Authorization", "Bearer $ownerToken")
        } When {
            post("/api/applications/$appId/withdraw")
        } Then {
            statusCode(403)
            body("detail", containsString("not the applicant"))
        }
    }

    @Test
    fun `withdraw non-existent application returns 404`() {
        Given {
            header("Authorization", "Bearer $applicantToken")
        } When {
            post("/api/applications/99999/withdraw")
        } Then {
            statusCode(404)
        }
    }

    @Test
    fun `withdraw already reviewed application returns 409`() {
        val listingId = createActiveListing(ownerToken)
        val appId = submitApplication(applicantToken, listingId)

        Given {
            header("Authorization", "Bearer $ownerToken")
            queryParam("decision", "APPROVE")
        } When {
            patch("/api/applications/$appId/review")
        } Then {
            statusCode(200)
        }

        Given {
            header("Authorization", "Bearer $applicantToken")
        } When {
            post("/api/applications/$appId/withdraw")
        } Then {
            statusCode(409)
            body("detail", containsString("cannot be withdrawn in its current status"))
        }
    }
}
