package com.sorsix.pawconnect.api

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
class PetControllerTest {
    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var dataSource: DataSource

    @Autowired
    private lateinit var testBlobStorageService: TestBlobStorageService

    @MockitoBean
    private lateinit var emailService: EmailService

    private lateinit var ownerToken: String
    private var petId: Long = 0L

    @BeforeEach
    fun setup() {
        RestAssured.port = port
        cleanDatabase()
        testBlobStorageService.reset()
        prepareOwnerAndPet()
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
                    """.trimIndent(),
                )
                statement.execute("SET session_replication_role = 'origin'")
            }
        }
    }

    private fun prepareOwnerAndPet() {
        val username = "petowner_${System.currentTimeMillis()}"
        val email = "$username@test.com"
        Given {
            body(
                """
                {
                    "username": "$username",
                    "email": "$email",
                    "password": "Password1!",
                    "firstName": "Pet",
                    "lastName": "Owner"
                }
                """.trimIndent(),
            )
            contentType(ContentType.JSON)
        } When {
            post("/api/auth/register")
        } Then {
            statusCode(201)
        }

        val loginResponse =
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

        ownerToken = loginResponse.getString("accessToken")

        val speciesCode = getFirstSpeciesCode()
        val municipalityCode = getFirstMunicipalityCode()

        val listingPayload =
            """
            {
                "pet": {
                    "name": "TestPet",
                    "speciesCode": "$speciesCode",
                    "gender": "MALE",
                    "size": "MEDIUM",
                    "description": "Test pet"
                },
                "municipalityCode": "$municipalityCode",
                "title": "Test Listing",
                "adoptionFee": 50,
                "saveAsDraft": true
            }
            """.trimIndent()

        petId = Given {
            header("Authorization", "Bearer $ownerToken")
            body(listingPayload)
            contentType(ContentType.JSON)
        } When {
            post("/api/listings")
        } Then {
            statusCode(201)
        } Extract {
            jsonPath().getLong("pet.id")
        }
    }

    private fun getFirstSpeciesCode(): String =
        Given {
            accept(ContentType.JSON)
        } When {
            get("/api/lookups/species")
        } Then {
            statusCode(200)
        } Extract {
            jsonPath().getString("[0].code")
        }

    private fun getFirstMunicipalityCode(): String =
        Given {
            accept(ContentType.JSON)
        } When {
            get("/api/lookups/municipalities")
        } Then {
            statusCode(200)
        } Extract {
            jsonPath().getString("[0].code")
        }

    private fun tempFile(
        name: String,
        bytes: ByteArray = byteArrayOf(1, 2, 3, 4),
    ): File {
        val f = File.createTempFile(name, ".tmp")
        f.writeBytes(bytes)
        f.deleteOnExit()
        return f
    }

    @Test
    fun `get pet by id returns pet details`() {
        Given {
            header("Authorization", "Bearer $ownerToken")
        } When {
            get("/api/pets/$petId")
        } Then {
            statusCode(200)
            body("id", equalTo(petId.toInt()))
            body("name", equalTo("TestPet"))
            body("speciesCode", notNullValue())
        }
    }

    @Test
    fun `update pet modifies fields`() {
        val updatePayload =
            """
            {
                "name": "UpdatedName",
                "description": "New description",
                "goodWithKids": true
            }
            """.trimIndent()

        Given {
            header("Authorization", "Bearer $ownerToken")
            body(updatePayload)
            contentType(ContentType.JSON)
        } When {
            put("/api/pets/$petId")
        } Then {
            statusCode(200)
            body("name", equalTo("UpdatedName"))
            body("description", equalTo("New description"))
            body("goodWithKids", equalTo(true))
        }
    }

    @Test
    fun `add photo to pet returns photo details`() {
        val photoPayload =
            """
            {
                "url": "https://example.com/photo.jpg",
                "isPrimary": true
            }
            """.trimIndent()

        Given {
            header("Authorization", "Bearer $ownerToken")
            body(photoPayload)
            contentType(ContentType.JSON)
        } When {
            post("/api/pets/$petId/photos")
        } Then {
            statusCode(201)
            body("url", equalTo("https://example.com/photo.jpg"))
            body("isPrimary", equalTo(true))
        }
    }

    @Test
    fun `remove photo from pet returns 204`() {
        val photoPayload = """{"url":"https://example.com/photo2.jpg","isPrimary":false}"""
        val photoId =
            Given {
                header("Authorization", "Bearer $ownerToken")
                body(photoPayload)
                contentType(ContentType.JSON)
            } When {
                post("/api/pets/$petId/photos")
            } Then {
                statusCode(201)
            } Extract {
                jsonPath().getLong("id")
            }

        Given {
            header("Authorization", "Bearer $ownerToken")
        } When {
            delete("/api/pets/$petId/photos/$photoId")
        } Then {
            statusCode(204)
        }

        Given {
            header("Authorization", "Bearer $ownerToken")
        } When {
            get("/api/pets/$petId")
        } Then {
            statusCode(200)
            body("photos", hasSize<Any>(0))
        }
    }

    @Test
    fun `unauthorized user cannot update pet`() {
        val otherUsername = "other_${System.currentTimeMillis()}"
        Given {
            body(
                """
                {
                    "username": "$otherUsername",
                    "email": "$otherUsername@test.com",
                    "password": "Password1!"
                }
                """.trimIndent(),
            )
            contentType(ContentType.JSON)
        } When {
            post("/api/auth/register")
        } Then {
            statusCode(201)
        }

        val otherToken =
            Given {
                body("""{"username":"$otherUsername","password":"Password1!"}""")
                contentType(ContentType.JSON)
            } When {
                post("/api/auth/login")
            } Then {
                statusCode(200)
            } Extract {
                jsonPath().getString("accessToken")
            }

        val updatePayload = """{"name":"Hacked"}"""
        Given {
            header("Authorization", "Bearer $otherToken")
            body(updatePayload)
            contentType(ContentType.JSON)
        } When {
            put("/api/pets/$petId")
        } Then {
            statusCode(403)
            body("detail", equalTo("You do not own this pet"))
        }
    }

    @Test
    fun `upload photo to pet returns 201 with blob url`() {
        val file = tempFile("photo")

        Given {
            header("Authorization", "Bearer $ownerToken")
            multiPart("file", file, "image/jpeg")
            queryParam("isPrimary", true)
        } When {
            post("/api/pets/$petId/photos/upload")
        } Then {
            statusCode(201)
            body("url", startsWith("http://fake-blob.test/"))
            body("isPrimary", equalTo(true))
        }

        assert(testBlobStorageService.uploadedUrls.size == 1)
    }

    @Test
    fun `uploading a second primary photo unsets the previous primary`() {
        val first = tempFile("first")
        val firstId =
            Given {
                header("Authorization", "Bearer $ownerToken")
                multiPart("file", first, "image/jpeg")
                queryParam("isPrimary", true)
            } When {
                post("/api/pets/$petId/photos/upload")
            } Then {
                statusCode(201)
            } Extract {
                jsonPath().getLong("id")
            }

        val second = tempFile("second")
        val secondId =
            Given {
                header("Authorization", "Bearer $ownerToken")
                multiPart("file", second, "image/jpeg")
                queryParam("isPrimary", true)
            } When {
                post("/api/pets/$petId/photos/upload")
            } Then {
                statusCode(201)
            } Extract {
                jsonPath().getLong("id")
            }

        Given {
            header("Authorization", "Bearer $ownerToken")
        } When {
            get("/api/pets/$petId")
        } Then {
            statusCode(200)
            body("photos.find { it.id == $firstId }.isPrimary", equalTo(false))
            body("photos.find { it.id == $secondId }.isPrimary", equalTo(true))
        }
    }

    @Test
    fun `uploading unsupported file type returns 400`() {
        val file = tempFile("notes")

        Given {
            header("Authorization", "Bearer $ownerToken")
            multiPart("file", file, "text/plain")
        } When {
            post("/api/pets/$petId/photos/upload")
        } Then {
            statusCode(400)
        }
    }

    @Test
    fun `uploading oversized file returns 413`() {
        val bigFile = tempFile("huge", ByteArray(6 * 1024 * 1024))

        Given {
            header("Authorization", "Bearer $ownerToken")
            multiPart("file", bigFile, "image/jpeg")
        } When {
            post("/api/pets/$petId/photos/upload")
        } Then {
            statusCode(413)
        }
    }

    @Test
    fun `uploading photo without auth returns 401`() {
        val file = tempFile("photo")

        Given {
            multiPart("file", file, "image/jpeg")
        } When {
            post("/api/pets/$petId/photos/upload")
        } Then {
            statusCode(401)
        }
    }

    @Test
    fun `removing an uploaded photo deletes it from blob storage`() {
        val file = tempFile("photo")
        val photoUrl =
            Given {
                header("Authorization", "Bearer $ownerToken")
                multiPart("file", file, "image/jpeg")
            } When {
                post("/api/pets/$petId/photos/upload")
            } Then {
                statusCode(201)
            } Extract {
                jsonPath()
            }

        val photoId = photoUrl.getLong("id")
        val url = photoUrl.getString("url")

        Given {
            header("Authorization", "Bearer $ownerToken")
        } When {
            delete("/api/pets/$petId/photos/$photoId")
        } Then {
            statusCode(204)
        }

        assert(testBlobStorageService.deletedUrls.contains(url)) {
            "Expected $url to have been passed to BlobStorageService.delete()"
        }
    }

    @Test
    fun `non-owner cannot upload photo to pet`() {
        val otherUsername = "otherphoto_${System.currentTimeMillis()}"
        Given {
            body(
                """
                {
                    "username": "$otherUsername",
                    "email": "$otherUsername@test.com",
                    "password": "Password1!"
                }
                """.trimIndent(),
            )
            contentType(ContentType.JSON)
        } When {
            post("/api/auth/register")
        } Then {
            statusCode(201)
        }

        val otherToken =
            Given {
                body("""{"username":"$otherUsername","password":"Password1!"}""")
                contentType(ContentType.JSON)
            } When {
                post("/api/auth/login")
            } Then {
                statusCode(200)
            } Extract {
                jsonPath().getString("accessToken")
            }

        val file = tempFile("photo")
        Given {
            header("Authorization", "Bearer $otherToken")
            multiPart("file", file, "image/jpeg")
        } When {
            post("/api/pets/$petId/photos/upload")
        } Then {
            statusCode(403)
        }
    }
}
