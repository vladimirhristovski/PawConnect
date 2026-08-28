package com.sorsix.pawconnect.web

import com.sorsix.pawconnect.TestcontainersConfiguration
import com.sorsix.pawconnect.service.EmailService
import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.restassured.module.kotlin.extensions.*
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
class AdminControllerTest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var dataSource: DataSource

    @MockitoBean
    private lateinit var emailService: EmailService

    private lateinit var adminToken: String
    private lateinit var plainToken: String
    private lateinit var plainUsername: String
    private var plainUserId: Long = 0L

    @BeforeEach
    fun setup() {
        RestAssured.port = port
        cleanDatabase()

        val adminUsername = "admin_${System.currentTimeMillis()}"
        adminToken = registerAndLogin(adminUsername)
        grantAdminRole(adminUsername)

        plainUsername = "plain_${System.currentTimeMillis()}"
        plainToken = registerAndLogin(plainUsername)
        plainUserId = getUserId(plainUsername)
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

    private fun grantAdminRole(username: String) {
        dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                statement.execute(
                    """
                    INSERT INTO user_roles (user_id, role_id)
                    SELECT u.id, r.id FROM users u, roles r
                    WHERE u.username = '$username' AND r.name = 'ADMIN'
                    ON CONFLICT (user_id, role_id) DO NOTHING
                    """.trimIndent()
                )
            }
        }
    }

    private fun getUserId(username: String): Long {
        dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT id FROM users WHERE username = '$username'").use { rs ->
                    rs.next()
                    return rs.getLong("id")
                }
            }
        }
    }

    private fun getFirstSpeciesCode(): String {
        return Given {
            accept(ContentType.JSON)
        } When {
            get("/api/lookups/species")
        } Then {
            statusCode(200)
        } Extract {
            jsonPath().getString("[0].code")
        }
    }

    private fun getFirstMunicipalityCode(): String {
        return Given {
            accept(ContentType.JSON)
        } When {
            get("/api/lookups/municipalities")
        } Then {
            statusCode(200)
        } Extract {
            jsonPath().getString("[0].code")
        }
    }

    private fun getFirstBusinessTypeCode(): String {
        return Given {
            accept(ContentType.JSON)
        } When {
            get("/api/lookups/business-types")
        } Then {
            statusCode(200)
        } Extract {
            jsonPath().getString("[0].code")
        }
    }

    private fun createBusiness(token: String): Long {
        val payload = """
        {
            "name": "Admin Test Business",
            "typeCode": "${getFirstBusinessTypeCode()}",
            "municipalityCode": "${getFirstMunicipalityCode()}",
            "address": "123 Main St",
            "phone": "123456789"
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

    private fun createListing(token: String, petName: String, draft: Boolean): Long {
        val payload = """
        {
            "pet": {
                "name": "$petName",
                "speciesCode": "${getFirstSpeciesCode()}",
                "gender": "MALE",
                "size": "MEDIUM"
            },
            "municipalityCode": "${getFirstMunicipalityCode()}",
            "title": "$petName listing",
            "adoptionFee": 20,
            "saveAsDraft": $draft
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

    private fun registerAndLogin(username: String): String {
        Given {
            body(
                """
                {
                    "username": "$username",
                    "email": "$username@test.com",
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

        return Given {
            body("""{"username":"$username","password":"Password1!"}""")
            contentType(ContentType.JSON)
        } When {
            post("/api/auth/login")
        } Then {
            statusCode(200)
        } Extract {
            jsonPath().getString("accessToken")
        }
    }

    @Test
    fun `admin can list users`() {
        Given {
            header("Authorization", "Bearer $adminToken")
        } When {
            get("/api/admin/users")
        } Then {
            statusCode(200)
            body("content.size()", greaterThanOrEqualTo(2))
        }
    }

    @Test
    fun `admin can filter users by role`() {
        Given {
            header("Authorization", "Bearer $adminToken")
            queryParam("role", "ADMIN")
        } When {
            get("/api/admin/users")
        } Then {
            statusCode(200)
            body("content.every { it.roles.contains('ADMIN') }", equalTo(true))
        }
    }

    @Test
    fun `non-admin cannot list users`() {
        Given {
            header("Authorization", "Bearer $plainToken")
        } When {
            get("/api/admin/users")
        } Then {
            statusCode(403)
        }
    }

    @Test
    fun `unauthenticated request cannot list users`() {
        Given {
            accept(ContentType.JSON)
        } When {
            get("/api/admin/users")
        } Then {
            statusCode(403)
        }
    }

    @Test
    fun `admin can deactivate a user`() {
        Given {
            header("Authorization", "Bearer $adminToken")
            body("""{"active": false}""")
            contentType(ContentType.JSON)
        } When {
            patch("/api/admin/users/$plainUserId/status")
        } Then {
            statusCode(200)
            body("isActive", equalTo(false))
        }
    }

    @Test
    fun `deactivated user's existing token stops working immediately`() {
        Given {
            header("Authorization", "Bearer $adminToken")
            body("""{"active": false}""")
            contentType(ContentType.JSON)
        } When {
            patch("/api/admin/users/$plainUserId/status")
        } Then {
            statusCode(200)
        }

        Given {
            header("Authorization", "Bearer $plainToken")
        } When {
            get("/api/pets/1")
        } Then {
            statusCode(anyOf(equalTo(403), equalTo(404)))
        }
    }

    @Test
    fun `deactivated user's refresh token stops working immediately`() {
        val refreshToken = Given {
            body("""{"username":"$plainUsername","password":"Password1!"}""")
            contentType(ContentType.JSON)
        } When {
            post("/api/auth/login")
        } Then {
            statusCode(200)
        } Extract {
            jsonPath().getString("refreshToken")
        }

        Given {
            header("Authorization", "Bearer $adminToken")
            body("""{"active": false}""")
            contentType(ContentType.JSON)
        } When {
            patch("/api/admin/users/$plainUserId/status")
        } Then {
            statusCode(200)
        }

        Given {
            body(mapOf("refreshToken" to refreshToken))
            contentType(ContentType.JSON)
        } When {
            post("/api/auth/refresh")
        } Then {
            statusCode(400)
        }
    }

    @Test
    fun `deactivated user cannot log in again`() {
        Given {
            header("Authorization", "Bearer $adminToken")
            body("""{"active": false}""")
            contentType(ContentType.JSON)
        } When {
            patch("/api/admin/users/$plainUserId/status")
        } Then {
            statusCode(200)
        }

        Given {
            body("""{"username":"$plainUsername","password":"Password1!"}""")
            contentType(ContentType.JSON)
        } When {
            post("/api/auth/login")
        } Then {
            statusCode(401)
        }
    }

    @Test
    fun `admin can reactivate a user`() {
        Given {
            header("Authorization", "Bearer $adminToken")
            body("""{"active": false}""")
            contentType(ContentType.JSON)
        } When {
            patch("/api/admin/users/$plainUserId/status")
        } Then {
            statusCode(200)
        }

        Given {
            header("Authorization", "Bearer $adminToken")
            body("""{"active": true}""")
            contentType(ContentType.JSON)
        } When {
            patch("/api/admin/users/$plainUserId/status")
        } Then {
            statusCode(200)
            body("isActive", equalTo(true))
        }
    }

    @Test
    fun `updating status for a nonexistent user returns 404`() {
        Given {
            header("Authorization", "Bearer $adminToken")
            body("""{"active": false}""")
            contentType(ContentType.JSON)
        } When {
            patch("/api/admin/users/999999/status")
        } Then {
            statusCode(404)
        }
    }

    @Test
    fun `non-admin cannot deactivate a user`() {
        Given {
            header("Authorization", "Bearer $plainToken")
            body("""{"active": false}""")
            contentType(ContentType.JSON)
        } When {
            patch("/api/admin/users/$plainUserId/status")
        } Then {
            statusCode(403)
        }
    }

    @Test
    fun `admin can delete a user`() {
        Given {
            header("Authorization", "Bearer $adminToken")
        } When {
            delete("/api/admin/users/$plainUserId")
        } Then {
            statusCode(204)
        }

        Given {
            body("""{"username":"$plainUsername","password":"Password1!"}""")
            contentType(ContentType.JSON)
        } When {
            post("/api/auth/login")
        } Then {
            statusCode(401)
        }

        Given {
            header("Authorization", "Bearer $plainToken")
        } When {
            get("/api/pets/1")
        } Then {
            statusCode(anyOf(equalTo(403), equalTo(404)))
        }
    }

    @Test
    fun `deleting a nonexistent user returns 404`() {
        Given {
            header("Authorization", "Bearer $adminToken")
        } When {
            delete("/api/admin/users/999999")
        } Then {
            statusCode(404)
        }
    }

    @Test
    fun `non-admin cannot delete a user`() {
        Given {
            header("Authorization", "Bearer $plainToken")
        } When {
            delete("/api/admin/users/$plainUserId")
        } Then {
            statusCode(403)
        }
    }

    @Test
    fun `admin can see listings of any status including drafts`() {
        val draftId = createListing(plainToken, "DraftPet", draft = true)
        val activeId = createListing(plainToken, "ActivePet", draft = false)

        Given {
            header("Authorization", "Bearer $adminToken")
        } When {
            get("/api/admin/listings")
        } Then {
            statusCode(200)
            body("content.id", hasItems(draftId.toInt(), activeId.toInt()))
        }
    }

    @Test
    fun `admin can filter listings by status`() {
        createListing(plainToken, "DraftPet2", draft = true)
        val activeId = createListing(plainToken, "ActivePet2", draft = false)

        Given {
            header("Authorization", "Bearer $adminToken")
            queryParam("status", "ACTIVE")
        } When {
            get("/api/admin/listings")
        } Then {
            statusCode(200)
            body("content.every { it.statusCode == 'ACTIVE' }", equalTo(true))
            body("content.id", hasItem(activeId.toInt()))
        }
    }

    @Test
    fun `admin can see a listing posted by a business account`() {
        val businessId = createBusiness(plainToken)
        val payload = """
        {
            "pet": {
                "name": "BizPet",
                "speciesCode": "${getFirstSpeciesCode()}",
                "gender": "MALE",
                "size": "MEDIUM"
            },
            "businessId": $businessId,
            "municipalityCode": "${getFirstMunicipalityCode()}",
            "title": "Business listing",
            "adoptionFee": 20,
            "saveAsDraft": false
        }
        """.trimIndent()

        val listingId = Given {
            header("Authorization", "Bearer $plainToken")
            body(payload)
            contentType(ContentType.JSON)
        } When {
            post("/api/listings")
        } Then {
            statusCode(201)
        } Extract {
            jsonPath().getLong("id")
        }

        Given {
            header("Authorization", "Bearer $adminToken")
        } When {
            get("/api/admin/listings")
        } Then {
            statusCode(200)
            body("content.find { it.id == $listingId }.business.id", equalTo(businessId.toInt()))
            body("content.find { it.id == $listingId }.business.ownerUsername", equalTo(plainUsername))
        }
    }

    @Test
    fun `non-admin cannot list all listings`() {
        Given {
            header("Authorization", "Bearer $plainToken")
        } When {
            get("/api/admin/listings")
        } Then {
            statusCode(403)
        }
    }

    @Test
    fun `admin can see all applications`() {
        val listingId = createListing(plainToken, "ApplyPet", draft = false)

        val applicantUsername = "applicant_${System.currentTimeMillis()}"
        val applicantToken = registerAndLogin(applicantUsername)

        Given {
            header("Authorization", "Bearer $applicantToken")
            body("""{"message": "I would love to adopt"}""")
            contentType(ContentType.JSON)
        } When {
            post("/api/listings/$listingId/applications")
        } Then {
            statusCode(201)
        }

        Given {
            header("Authorization", "Bearer $adminToken")
        } When {
            get("/api/admin/applications")
        } Then {
            statusCode(200)
            body("content.size()", greaterThanOrEqualTo(1))
            body("content.find { it.listingId == $listingId }.applicantUsername", equalTo(applicantUsername))
        }
    }

    @Test
    fun `non-admin cannot list all applications`() {
        Given {
            header("Authorization", "Bearer $plainToken")
        } When {
            get("/api/admin/applications")
        } Then {
            statusCode(403)
        }
    }
}
