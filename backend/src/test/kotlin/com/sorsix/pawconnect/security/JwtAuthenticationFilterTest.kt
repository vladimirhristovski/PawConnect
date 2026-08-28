package com.sorsix.pawconnect.security

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.FilterChain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class JwtAuthenticationFilterTest {

    private val jwtService = mockk<JwtService>()
    private val userDetailsService = mockk<UserDetailsService>()
    private val filter = JwtAuthenticationFilter(jwtService, userDetailsService)

    @AfterEach
    fun clearContext() = SecurityContextHolder.clearContext()

    private fun invoke(authHeader: String?) {
        val request = MockHttpServletRequest()
        authHeader?.let { request.addHeader("Authorization", it) }
        val response = MockHttpServletResponse()
        val chain = mockk<FilterChain>(relaxed = true)
        filter.doFilter(request, response, chain)
        verify { chain.doFilter(request, response) }
    }

    @Test
    fun `request without an Authorization header stays unauthenticated`() {
        invoke(null)
        assertNull(SecurityContextHolder.getContext().authentication)
    }

    @Test
    fun `non-bearer Authorization header is ignored`() {
        invoke("Basic dXNlcjpwYXNz")
        assertNull(SecurityContextHolder.getContext().authentication)
    }

    @Test
    fun `invalid access token does not populate the context`() {
        every { jwtService.validateAccessToken("bad") } returns false
        invoke("Bearer bad")
        assertNull(SecurityContextHolder.getContext().authentication)
    }

    @Test
    fun `valid token for an enabled user authenticates the request`() {
        every { jwtService.validateAccessToken("good") } returns true
        every { jwtService.extractUsername("good") } returns "alice"
        val userDetails = mockk<UserDetails>(relaxed = true)
        every { userDetails.isEnabled } returns true
        every { userDetails.authorities } returns emptyList()
        every { userDetailsService.loadUserByUsername("alice") } returns userDetails

        invoke("Bearer good")

        assertNotNull(SecurityContextHolder.getContext().authentication)
    }

    @Test
    fun `valid token for a disabled user is rejected while the chain still runs`() {
        every { jwtService.validateAccessToken("good") } returns true
        every { jwtService.extractUsername("good") } returns "bob"
        val userDetails = mockk<UserDetails>(relaxed = true)
        every { userDetails.isEnabled } returns false
        every { userDetailsService.loadUserByUsername("bob") } returns userDetails

        invoke("Bearer good")

        assertNull(SecurityContextHolder.getContext().authentication)
    }
}
