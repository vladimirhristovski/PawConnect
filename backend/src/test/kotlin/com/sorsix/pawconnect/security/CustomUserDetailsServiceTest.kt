package com.sorsix.pawconnect.security

import com.sorsix.pawconnect.model.Role
import com.sorsix.pawconnect.model.User
import com.sorsix.pawconnect.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.security.core.userdetails.UsernameNotFoundException
import java.util.*
import java.time.Instant

class CustomUserDetailsServiceTest {

    private val userRepository: UserRepository = mockk()
    private val userDetailsService = CustomUserDetailsService(userRepository)

    @Test
    fun `loadUserByUsername should return CustomUserDetails when user exists`() {
        val user = User(
            username = "john",
            email = "john@mail.com",
            password = "encoded",
            firstName = "John",
            lastName = "Doe",
            phone = "123"
        ).apply {
            id = 1L
            isActive = true
            roles.add(Role("USER").apply { id = 1L })
        }
        every { userRepository.findByUsernameActive("john") } returns Optional.of(user)

        val details = userDetailsService.loadUserByUsername("john")
        assertTrue(details is CustomUserDetails)
        assertEquals("john", details.username)
        assertEquals("encoded", details.password)
        assertEquals(1, details.authorities.size)
        assertTrue(details.authorities.any { it.authority == "ROLE_USER" })
        assertTrue(details.isEnabled)
        assertTrue(details.isAccountNonExpired)
        assertTrue(details.isAccountNonLocked)
        assertTrue(details.isCredentialsNonExpired)
    }

    @Test
    fun `loadUserByUsername should throw when user not found`() {
        every { userRepository.findByUsernameActive("unknown") } returns Optional.empty()
        assertThrows(UsernameNotFoundException::class.java) {
            userDetailsService.loadUserByUsername("unknown")
        }
    }

    @Test
    fun `loadUserByUsername should return disabled user as not enabled`() {
        val user = User("john", "john@mail.com", "encoded", null, null, null).apply {
            id = 1L
            isActive = false
        }
        every { userRepository.findByUsernameActive("john") } returns Optional.of(user)

        val details = userDetailsService.loadUserByUsername("john")
        assertFalse(details.isEnabled)
    }

    @Test
    fun `loadUserByUsername should consider deleted user as disabled`() {
        val user = User("john", "john@mail.com", "encoded", null, null, null).apply {
            id = 1L
            isActive = true
            deletedAt = Instant.now()
        }
        every { userRepository.findByUsernameActive("john") } returns Optional.of(user)

        val details = userDetailsService.loadUserByUsername("john")
        assertFalse(details.isEnabled)
    }
}