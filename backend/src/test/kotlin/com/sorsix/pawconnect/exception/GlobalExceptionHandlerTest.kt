package com.sorsix.pawconnect.exception

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.core.MethodParameter
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpInputMessage
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.DisabledException
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.multipart.MaxUploadSizeExceededException
import java.net.URI

class GlobalExceptionHandlerTest {
    private val handler = GlobalExceptionHandler()

    @Test
    fun `handleIllegalArgument returns BAD_REQUEST`() {
        val ex = IllegalArgumentException("Username taken")
        val response = handler.handleIllegalArgument(ex)
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("Username taken", response.body?.detail)
        assertEquals(URI.create("about:blank"), response.body?.type)
    }

    @Test
    fun `handleBadCredentials returns UNAUTHORIZED`() {
        val ex = BadCredentialsException("Invalid creds")
        val response = handler.handleBadCredentials(ex)
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        assertEquals("Invalid username or password", response.body?.detail)
    }

    @Test
    fun `handleValidation returns BAD_REQUEST with errors property`() {
        val fieldError = FieldError("object", "username", "must not be blank")
        val bindingResult =
            mockk<BindingResult> {
                every { allErrors } returns listOf(fieldError)
            }
        val parameter = mockk<MethodParameter>(relaxed = true)
        val ex = MethodArgumentNotValidException(parameter, bindingResult)
        val response =
            handler.handleMethodArgumentNotValid(
                ex,
                HttpHeaders(),
                HttpStatus.BAD_REQUEST,
                mockk(relaxed = true),
            )!!
        val body = response.body as ProblemDetail

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("Validation failed", body.detail)
        val errors = body.properties?.get("errors") as List<*>
        assertTrue(errors.isNotEmpty())
        val first = errors.first() as Map<*, *>
        assertEquals("username", first["field"])
        assertEquals("must not be blank", first["message"])
    }

    @Test
    fun `handleHttpMessageNotReadable returns BAD_REQUEST`() {
        val httpInputMessage = mockk<HttpInputMessage>()
        val ex = HttpMessageNotReadableException("Malformed JSON", httpInputMessage)
        val response =
            handler.handleHttpMessageNotReadable(
                ex,
                HttpHeaders(),
                HttpStatus.BAD_REQUEST,
                mockk(relaxed = true),
            )!!
        val body = response.body as ProblemDetail
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("Malformed JSON request", body.detail)
        assertEquals(URI.create("about:blank"), body.type)
    }

    @Test
    fun `handleGeneric returns INTERNAL_SERVER_ERROR`() {
        val ex = RuntimeException("Unexpected")
        val response = handler.handleGeneric(ex)
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals("An unexpected error occurred", response.body?.detail)
    }

    @Test
    fun `handleDisabled returns UNAUTHORIZED`() {
        val response = handler.handleDisabled(DisabledException("disabled"))
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        assertEquals("Account is deactivated", response.body?.detail)
    }

    @Test
    fun `handleTypeMismatch returns BAD_REQUEST with parameter and value properties`() {
        val ex = mockk<MethodArgumentTypeMismatchException>(relaxed = true)
        every { ex.name } returns "id"
        every { ex.value } returns "abc"
        every { ex.requiredType } returns Long::class.javaObjectType

        val response = handler.handleTypeMismatch(ex)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("id", response.body?.properties?.get("parameter"))
        assertEquals("abc", response.body?.properties?.get("value"))
    }

    @Test
    fun `handleMaxUploadSize returns PAYLOAD_TOO_LARGE`() {
        val response =
            handler.handleMaxUploadSizeExceededException(
                MaxUploadSizeExceededException(5),
                HttpHeaders(),
                HttpStatus.PAYLOAD_TOO_LARGE,
                mockk(relaxed = true),
            )!!
        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, response.statusCode)
    }

    @Test
    fun `handleBlobStorage returns BAD_GATEWAY`() {
        val response = handler.handleBlobStorage(BlobStorageException("blob down"))
        assertEquals(HttpStatus.BAD_GATEWAY, response.statusCode)
        assertEquals("blob down", response.body?.detail)
    }

    @Test
    fun `handleUnauthorized returns UNAUTHORIZED`() {
        val response = handler.handleUnauthorized(UnauthorizedException("no session"))
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        assertEquals("no session", response.body?.detail)
    }
}
