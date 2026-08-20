package com.sorsix.pawconnect.exception

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.context.request.WebRequest
import java.net.URI

class GlobalExceptionHandlerTest {

    private val handler = GlobalExceptionHandler()
    private val request = mockk<WebRequest> {
        every { getDescription(false) } returns "uri=/test"
    }

    @Test
    fun `handleIllegalArgument returns BAD_REQUEST`() {
        val ex = IllegalArgumentException("Username taken")
        val response = handler.handleIllegalArgument(ex, request)
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("Username taken", response.body?.detail)
        assertEquals(URI.create("about:blank"), response.body?.type)
    }

    @Test
    fun `handleBadCredentials returns UNAUTHORIZED`() {
        val ex = BadCredentialsException("Invalid creds")
        val response = handler.handleBadCredentials(ex, request)
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        assertEquals("Invalid username or password", response.body?.detail)
    }

    @Test
    fun `handleValidation returns BAD_REQUEST with errors property`() {
        val fieldError = FieldError("object", "username", "must not be blank")
        val bindingResult = mockk<BindingResult> {
            every { allErrors } returns listOf(fieldError)
        }
        val ex = mockk<MethodArgumentNotValidException> {
            every { getBindingResult() } returns bindingResult
        }
        val response = handler.handleValidation(ex, request)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("Validation failed", response.body?.detail)
        val errors = response.body?.properties?.get("errors") as List<*>
        assertTrue(errors.isNotEmpty())
        val first = errors.first() as Map<*, *>
        assertEquals("username", first["field"])
        assertEquals("must not be blank", first["message"])
    }

    @Test
    fun `handleGeneric returns INTERNAL_SERVER_ERROR`() {
        val ex = RuntimeException("Unexpected")
        val response = handler.handleGeneric(ex, request)
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals("An unexpected error occurred", response.body?.detail)
    }
}