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
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import javax.sql.DataSource

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class AuthControllerTest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var dataSource: DataSource

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @MockitoBean
    private lateinit var emailService: EmailService

    @BeforeEach
    fun setup() {
        RestAssured.port = port
        cleanDatabase()
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
                        users
                    RESTART IDENTITY CASCADE
                    """.trimIndent()
                )

                statement.execute("SET session_replication_role = 'origin'")
            }
        }
    }

    @Test
    fun `register new user should return 201 and user data`() {
        Given {
            body(
                """
                {
                    "username": "testuser",
                    "email": "test@example.com",
                    "password": "password123",
                    "firstName": "Test",
                    "lastName": "User",
                    "phone": "123456789"
                }
                """.trimIndent()
            )
            contentType(ContentType.JSON)
        } When {
            post("/api/auth/register")
        } Then {
            statusCode(201)
            body("username", equalTo("testuser"))
            body("email", equalTo("test@example.com"))
            body("firstName", equalTo("Test"))
            body("lastName", equalTo("User"))
            body("phone", equalTo("123456789"))
            body("roles", hasItem("USER"))
            body("isActive", equalTo(true))
        }
    }

    @Test
    fun `register with duplicate username should return 400`() {
        Given {
            body(
                """
                {
                    "username": "testuser",
                    "email": "test1@example.com",
                    "password": "password123"
                }
                """.trimIndent()
            )
            contentType(ContentType.JSON)
        } When {
            post("/api/auth/register")
        } Then {
            statusCode(201)
        }

        Given {
            body(
                """
                {
                    "username": "testuser",
                    "email": "test2@example.com",
                    "password": "password123"
                }
                """.trimIndent()
            )
            contentType(ContentType.JSON)
        } When {
            post("/api/auth/register")
        } Then {
            statusCode(409)
            body("detail", containsString("Username already taken"))
        }
    }

    @Test
    fun `login with valid credentials should return tokens and expiresIn`() {
        Given {
            body(
                """
                {
                    "username": "loginuser",
                    "email": "login@example.com",
                    "password": "secret123"
                }
                """.trimIndent()
            )
            contentType(ContentType.JSON)
        } When {
            post("/api/auth/register")
        } Then {
            statusCode(201)
        }

        val response = Given {
            body(
                """
                {
                    "username": "loginuser",
                    "password": "secret123"
                }
                """.trimIndent()
            )
            contentType(ContentType.JSON)
        } When {
            post("/api/auth/login")
        } Then {
            statusCode(200)
            body("accessToken", notNullValue())
            body("refreshToken", notNullValue())
            body("expiresIn", greaterThan(0))
        } Extract {
            jsonPath()
        }

        val accessToken = response.getString("accessToken")

        Given {
            header("Authorization", "Bearer $accessToken")
        } When {
            get("/api/auth/me")
        } Then {
            statusCode(200)
            body("username", equalTo("loginuser"))
        }
    }

    @Test
    fun `login with wrong password should return 401`() {
        Given {
            body(
                """
                {
                    "username": "wrongpass",
                    "email": "wrong@example.com",
                    "password": "correctpass"
                }
                """.trimIndent()
            )
            contentType(ContentType.JSON)
        } When {
            post("/api/auth/register")
        } Then {
            statusCode(201)
        }

        Given {
            body(
                """
                {
                    "username": "wrongpass",
                    "password": "wrongpassword"
                }
                """.trimIndent()
            )
            contentType(ContentType.JSON)
        } When {
            post("/api/auth/login")
        } Then {
            statusCode(401)
            body("detail", equalTo("Invalid username or password"))
        }
    }

    @Test
    fun `refresh token should return new pair and revoke old`() {
        Given {
            body(
                """
                {
                    "username": "refreshuser",
                    "email": "refresh@example.com",
                    "password": "password123"
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
            body(
                """
                {
                    "username": "refreshuser",
                    "password": "password123"
                }
                """.trimIndent()
            )
            contentType(ContentType.JSON)
        } When {
            post("/api/auth/login")
        } Then {
            statusCode(200)
        } Extract {
            jsonPath()
        }

        val oldRefreshToken = loginResponse.getString("refreshToken")
        val oldAccessToken = loginResponse.getString("accessToken")

        val refreshResponse = Given {
            body(
                mapOf(
                    "refreshToken" to oldRefreshToken
                )
            )
            contentType(ContentType.JSON)
        } When {
            post("/api/auth/refresh")
        } Then {
            statusCode(200)
            body("accessToken", not(equalTo(oldAccessToken)))
            body("refreshToken", not(equalTo(oldRefreshToken)))
        } Extract {
            jsonPath()
        }

        val newRefreshToken = refreshResponse.getString("refreshToken")

        Given {
            body(
                mapOf(
                    "refreshToken" to oldRefreshToken
                )
            )
            contentType(ContentType.JSON)
        } When {
            post("/api/auth/refresh")
        } Then {
            statusCode(400)
            body("detail", equalTo("Invalid or expired refresh token"))
        }

        Given {
            body(
                mapOf(
                    "refreshToken" to newRefreshToken
                )
            )
            contentType(ContentType.JSON)
        } When {
            post("/api/auth/refresh")
        } Then {
            statusCode(200)
            body("accessToken", notNullValue())
            body("refreshToken", notNullValue())
        }
    }

    @Test
    fun `logout should revoke refresh token`() {
        Given {
            body(
                """
                {
                    "username": "logoutuser",
                    "email": "logout@example.com",
                    "password": "password123"
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
            body(
                """
                {
                    "username": "logoutuser",
                    "password": "password123"
                }
                """.trimIndent()
            )
            contentType(ContentType.JSON)
        } When {
            post("/api/auth/login")
        } Then {
            statusCode(200)
        } Extract {
            jsonPath()
        }

        val refreshToken = loginResponse.getString("refreshToken")

        Given {
            body(
                mapOf(
                    "refreshToken" to refreshToken
                )
            )
            contentType(ContentType.JSON)
        } When {
            post("/api/auth/logout")
        } Then {
            statusCode(204)
        }

        Given {
            body(
                mapOf(
                    "refreshToken" to refreshToken
                )
            )
            contentType(ContentType.JSON)
        } When {
            post("/api/auth/refresh")
        } Then {
            statusCode(400)
            body("detail", equalTo("Invalid or expired refresh token"))
        }
    }

    @Test
    fun `get me with valid token returns user profile`() {
        Given {
            body(
                """
                {
                    "username": "meuser",
                    "email": "me@example.com",
                    "password": "password123",
                    "firstName": "Me",
                    "lastName": "User",
                    "phone": "5551234"
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
            body(
                """
                {
                    "username": "meuser",
                    "password": "password123"
                }
                """.trimIndent()
            )
            contentType(ContentType.JSON)
        } When {
            post("/api/auth/login")
        } Then {
            statusCode(200)
        } Extract {
            jsonPath()
        }

        val accessToken = loginResponse.getString("accessToken")

        Given {
            header("Authorization", "Bearer $accessToken")
        } When {
            get("/api/auth/me")
        } Then {
            statusCode(200)
            body("username", equalTo("meuser"))
            body("email", equalTo("me@example.com"))
            body("firstName", equalTo("Me"))
            body("lastName", equalTo("User"))
            body("phone", equalTo("5551234"))
            body("roles", hasItem("USER"))
            body("isActive", equalTo(true))
        }
    }

    @Test
    fun `get me without token returns 401`() {
        When {
            get("/api/auth/me")
        } Then {
            statusCode(401)
        }
    }

    @Test
    fun `forgot password for existing email should return 200`() {
        Given {
            body(
                """
                {
                    "username": "forgotuser",
                    "email": "forgot@example.com",
                    "password": "password123"
                }
                """.trimIndent()
            )
            contentType(ContentType.JSON)
        } When {
            post("/api/auth/register")
        } Then {
            statusCode(201)
        }

        Given {
            body(
                mapOf(
                    "email" to "forgot@example.com"
                )
            )
            contentType(ContentType.JSON)
        } When {
            post("/api/auth/forgot-password")
        } Then {
            statusCode(200)
        }

        verify(emailService).sendEmail(
            eq("forgot@example.com"),
            eq("Password Reset Request"),
            any()
        )
    }

    @Test
    fun `forgot password for non-existent email should return 200`() {
        Given {
            body(
                mapOf(
                    "email" to "nonexistent@example.com"
                )
            )
            contentType(ContentType.JSON)
        } When {
            post("/api/auth/forgot-password")
        } Then {
            statusCode(200)
        }

        verify(emailService, never()).sendEmail(
            any(),
            any(),
            any()
        )
    }

    @Test
    fun `delete account should soft delete user and revoke tokens`() {
        Given {
            body(
                """
                {
                    "username": "deleteuser",
                    "email": "delete@example.com",
                    "password": "password123"
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
            body(
                """
                {
                    "username": "deleteuser",
                    "password": "password123"
                }
                """.trimIndent()
            )
            contentType(ContentType.JSON)
        } When {
            post("/api/auth/login")
        } Then {
            statusCode(200)
        } Extract {
            jsonPath()
        }

        val accessToken = loginResponse.getString("accessToken")
        val refreshToken = loginResponse.getString("refreshToken")

        Given {
            header("Authorization", "Bearer $accessToken")
        } When {
            delete("/api/auth/me")
        } Then {
            statusCode(204)
        }

        Given {
            body(
                """
                {
                    "username": "deleteuser",
                    "password": "password123"
                }
                """.trimIndent()
            )
            contentType(ContentType.JSON)
        } When {
            post("/api/auth/login")
        } Then {
            statusCode(401)
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
    fun `delete account without token returns 401`() {
        When {
            delete("/api/auth/me")
        } Then {
            statusCode(401)
        }
    }

    @Test
    fun `reset password with invalid token should return 400`() {
        Given {
            body(
                mapOf(
                    "token" to "invalid-token-123",
                    "newPassword" to "newSecurePassword123"
                )
            )
            contentType(ContentType.JSON)
        } When {
            post("/api/auth/reset-password")
        } Then {
            statusCode(400)
            body("detail", equalTo("Invalid token"))
        }
    }
}