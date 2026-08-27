package com.sorsix.pawconnect.web

import com.sorsix.pawconnect.TestcontainersConfiguration
import com.sorsix.pawconnect.service.EmailService
import com.sorsix.pawconnect.service.TestBlobStorageService
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
import java.io.File
import javax.sql.DataSource

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class PhotoUploadControllerTest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var dataSource: DataSource

    @Autowired
    private lateinit var testBlobStorageService: TestBlobStorageService

    @MockitoBean
    private lateinit var emailService: EmailService

    private lateinit var userToken: String

    @BeforeEach
    fun setup() {
        RestAssured.port = port
        cleanDatabase()
        testBlobStorageService.reset()
        userToken = registerAndLogin("photouploader_${System.currentTimeMillis()}")
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

    private fun registerAndLogin(username: String): String {
        Given {
            body(
                """
                {
                    "username": "$username",
                    "email": "$username@test.com",
                    "password": "Password1!",
                    "firstName": "Photo",
                    "lastName": "Uploader"
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

    private fun tempFile(name: String, bytes: ByteArray = byteArrayOf(1, 2, 3, 4)): File {
        val f = File.createTempFile(name, ".tmp")
        f.writeBytes(bytes)
        f.deleteOnExit()
        return f
    }

    @Test
    fun `upload temp photo returns 201 with blob url`() {
        val file = tempFile("photo")

        Given {
            header("Authorization", "Bearer $userToken")
            multiPart("file", file, "image/jpeg")
        } When {
            post("/api/photos/upload")
        } Then {
            statusCode(201)
            body("url", startsWith("http://fake-blob.test/"))
            body("url", containsString("temp/"))
        }

        assert(testBlobStorageService.uploadedUrls.size == 1)
    }

    @Test
    fun `upload without auth returns 403`() {
        val file = tempFile("photo")

        Given {
            multiPart("file", file, "image/jpeg")
        } When {
            post("/api/photos/upload")
        } Then {
            statusCode(403)
        }

        assert(testBlobStorageService.uploadedUrls.isEmpty())
    }

    @Test
    fun `uploading unsupported file type returns 400`() {
        val file = tempFile("notes")

        Given {
            header("Authorization", "Bearer $userToken")
            multiPart("file", file, "text/plain")
        } When {
            post("/api/photos/upload")
        } Then {
            statusCode(400)
        }
    }

    @Test
    fun `uploading oversized file returns 413`() {
        val bigFile = tempFile("huge", ByteArray(6 * 1024 * 1024))

        Given {
            header("Authorization", "Bearer $userToken")
            multiPart("file", bigFile, "image/jpeg")
        } When {
            post("/api/photos/upload")
        } Then {
            statusCode(413)
        }
    }
}
