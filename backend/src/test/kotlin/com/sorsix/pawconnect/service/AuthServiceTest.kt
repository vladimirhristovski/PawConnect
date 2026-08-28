package com.sorsix.pawconnect.service

import com.sorsix.pawconnect.dto.request.*
import com.sorsix.pawconnect.domain.PasswordResetToken
import com.sorsix.pawconnect.domain.RefreshToken
import com.sorsix.pawconnect.domain.Role
import com.sorsix.pawconnect.domain.User
import com.sorsix.pawconnect.repository.PasswordResetTokenRepository
import com.sorsix.pawconnect.repository.RefreshTokenRepository
import com.sorsix.pawconnect.repository.RoleRepository
import com.sorsix.pawconnect.repository.UserRepository
import com.sorsix.pawconnect.security.CustomUserDetails
import com.sorsix.pawconnect.security.JwtService
import com.sorsix.pawconnect.common.requireId
import io.mockk.*
import org.hibernate.exception.ConstraintViolationException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Instant
import java.util.*

class AuthServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var refreshTokenRepository: RefreshTokenRepository
    private lateinit var roleRepository: RoleRepository
    private lateinit var passwordEncoder: PasswordEncoder
    private lateinit var jwtService: JwtService
    private lateinit var authenticationManager: AuthenticationManager
    private lateinit var passwordResetTokenRepository: PasswordResetTokenRepository
    private lateinit var emailService: EmailService
    private lateinit var userService: UserService
    private lateinit var authService: AuthService

    private val resetTokenTtl = 3600000L

    @BeforeEach
    fun setup() {
        userRepository = mockk()
        refreshTokenRepository = mockk()
        roleRepository = mockk()
        passwordEncoder = mockk()
        jwtService = mockk()
        authenticationManager = mockk()
        passwordResetTokenRepository = mockk()
        emailService = mockk(relaxed = true)
        userService = mockk()

        authService = AuthService(
            userRepository,
            refreshTokenRepository,
            roleRepository,
            passwordEncoder,
            jwtService,
            authenticationManager,
            passwordResetTokenRepository,
            emailService,
            userService,
            900_000L,
            resetTokenTtl,
            "http://localhost:4200"
        )
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `register should save user with USER role`() {
        val request = RegisterRequest(
            username = "john_doe",
            email = "john@example.com",
            password = "secret123",
            firstName = "John",
            lastName = "Doe",
            phone = "123456789"
        )
        val userRole = Role("USER").apply { id = 1L }
        val savedUser = User(
            username = request.username,
            email = request.email,
            password = "encoded",
            firstName = request.firstName,
            lastName = request.lastName,
            phone = request.phone
        ).apply { id = 100L; roles.add(userRole) }

        every { userRepository.existsByUsernameAndDeletedAtIsNull(request.username) } returns false
        every { userRepository.existsByEmailAndDeletedAtIsNull(request.email) } returns false
        every { roleRepository.findByName("USER") } returns Optional.of(userRole)
        every { passwordEncoder.encode(request.password) } returns "encoded"
        every { userRepository.save(any()) } returns savedUser

        val response = authService.register(request)

        assertEquals("john_doe", response.username)
        assertEquals("john@example.com", response.email)
        assertEquals("John", response.firstName)
        assertEquals("Doe", response.lastName)
        assertEquals("123456789", response.phone)
        assertTrue(response.roles.contains("USER"))
        verify(exactly = 1) { userRepository.save(any()) }
    }

    @Test
    fun `register should throw when username already taken`() {
        val request = RegisterRequest("john", "john@mail.com", "pass", null, null, null)
        every { userRepository.existsByUsernameAndDeletedAtIsNull(request.username) } returns true

        assertThrows(IllegalArgumentException::class.java) {
            authService.register(request)
        }
        verify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `register should throw when email already registered`() {
        val request = RegisterRequest("john", "john@mail.com", "pass", null, null, null)
        every { userRepository.existsByUsernameAndDeletedAtIsNull(request.username) } returns false
        every { userRepository.existsByEmailAndDeletedAtIsNull(request.email) } returns true

        assertThrows(IllegalArgumentException::class.java) {
            authService.register(request)
        }
        verify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `register should translate a racing duplicate username into a clean 400`() {
        val request = RegisterRequest("john", "john@mail.com", "pass", null, null, null)
        every { userRepository.existsByUsernameAndDeletedAtIsNull(request.username) } returns false
        every { userRepository.existsByEmailAndDeletedAtIsNull(request.email) } returns false
        every { roleRepository.findByName("USER") } returns Optional.of(Role("USER").apply { id = 1L })
        every { passwordEncoder.encode(request.password) } returns "encoded"
        val constraintViolation = ConstraintViolationException(
            "duplicate key", java.sql.SQLException("duplicate key"), "uq_users_username_active"
        )
        every { userRepository.save(any()) } throws DataIntegrityViolationException("insert failed", constraintViolation)

        val ex = assertThrows(IllegalArgumentException::class.java) {
            authService.register(request)
        }
        assertEquals("Username already taken", ex.message)
    }

    @Test
    fun `login should return tokens and revoke old refresh tokens`() {
        val request = LoginRequest("john", "secret")
        val user = User("john", "john@mail.com", "encoded", null, null, null)
            .apply { id = 1L; roles.add(Role("USER").apply { id = 1L }) }
        val authentication = mockk<Authentication> {
            every { principal } returns CustomUserDetails(user)
        }

        every { authenticationManager.authenticate(any<UsernamePasswordAuthenticationToken>()) } returns authentication
        every { refreshTokenRepository.revokeAllUserTokens(user.requireId(), any()) } returns 1
        every { jwtService.generateAccessToken(any()) } returns "access_token"
        val refreshPair = "raw_refresh" to mockk<RefreshToken>()
        every { jwtService.generateRefreshToken(user) } returns refreshPair
        every { refreshTokenRepository.save(refreshPair.second) } returns refreshPair.second

        val response = authService.login(request)

        assertEquals("access_token", response.accessToken)
        assertEquals("raw_refresh", response.refreshToken)
        assertEquals(900, response.expiresIn)
        verify { refreshTokenRepository.revokeAllUserTokens(user.requireId(), any()) }
        verify { jwtService.generateAccessToken(any()) }
    }

    @Test
    fun `login should throw BadCredentialsException when authentication fails`() {
        val request = LoginRequest("john", "wrong")
        every { authenticationManager.authenticate(any()) } throws BadCredentialsException("Bad credentials")

        assertThrows(BadCredentialsException::class.java) {
            authService.login(request)
        }
    }

    @Test
    fun `refreshToken should rotate tokens and revoke old refresh token`() {
        val user = User("john", "john@mail.com", "encoded", null, null, null).apply { id = 1L }

        val oldRefreshToken = RefreshToken(
            user = user,
            tokenHash = "hash",
            expiresAt = Instant.now().plusSeconds(3600),
            revokedAt = null
        )

        every { jwtService.verifyRefreshToken("old_token") } returns oldRefreshToken
        every { refreshTokenRepository.save(oldRefreshToken) } returns oldRefreshToken
        every { jwtService.generateAccessToken(any()) } returns "new_access"
        val newRefreshPair = "new_raw_refresh" to mockk<RefreshToken>()
        every { jwtService.generateRefreshToken(user) } returns newRefreshPair
        every { refreshTokenRepository.save(newRefreshPair.second) } returns newRefreshPair.second

        val response = authService.refreshToken("old_token")

        assertEquals("new_access", response.accessToken)
        assertEquals("new_raw_refresh", response.refreshToken)
        assertEquals(900, response.expiresIn)

        assertNotNull(oldRefreshToken.revokedAt)
        verify { refreshTokenRepository.save(oldRefreshToken) }
        verify { refreshTokenRepository.save(newRefreshPair.second) }
    }

    @Test
    fun `refreshToken should throw when token invalid`() {
        every { jwtService.verifyRefreshToken("invalid") } returns null
        assertThrows(IllegalArgumentException::class.java) {
            authService.refreshToken("invalid")
        }
    }

    @Test
    fun `logout should revoke refresh token`() {
        every { jwtService.revokeRefreshToken("token") } returns true
        val result = authService.logout("token")
        assertTrue(result)
    }

    @Test
    fun `forgotPassword should generate token, revoke old ones, and send email for existing user`() {
        val request = ForgotPasswordRequest("john@mail.com")
        val user = User("john", "john@mail.com", "pass", null, null, null).apply { id = 1L }

        every { userRepository.findByEmailActive(request.email) } returns Optional.of(user)
        every { passwordResetTokenRepository.revokeAllUnusedTokensForUser(user.requireId(), any()) } returns 1
        every { passwordResetTokenRepository.save(any<PasswordResetToken>()) } answers { it.invocation.args[0] as PasswordResetToken }

        val result = authService.forgotPassword(request)

        assertTrue(result)
        verify { passwordResetTokenRepository.revokeAllUnusedTokensForUser(user.requireId(), any()) }
        verify { passwordResetTokenRepository.save(any<PasswordResetToken>()) }
        verify(exactly = 1) { emailService.sendEmail("john@mail.com", "Password Reset Request", any()) }
    }

    @Test
    fun `forgotPassword should return true without sending email for non-existing user`() {
        val request = ForgotPasswordRequest("unknown@mail.com")
        every { userRepository.findByEmailActive(request.email) } returns Optional.empty()

        val result = authService.forgotPassword(request)

        assertTrue(result)
        verify { passwordResetTokenRepository wasNot called }
        verify(exactly = 0) { emailService.sendEmail(any(), any(), any()) }
    }

    @Test
    fun `resetPassword should update password and mark token used when valid`() {
        val user = User("john", "john@mail.com", "oldEncoded", null, null, null).apply { id = 1L }
        val rawToken = "rawToken"
        val tokenHash = authService.hashToken(rawToken)
        val resetToken = PasswordResetToken(
            user = user,
            tokenHash = tokenHash,
            expiresAt = Instant.now().plusSeconds(3600),
            usedAt = null
        )

        every { passwordResetTokenRepository.findByTokenHash(tokenHash) } returns Optional.of(resetToken)
        every { passwordEncoder.encode("newPass123") } returns "newEncoded"
        every { userRepository.save(user) } returns user
        every { passwordResetTokenRepository.save(resetToken) } returns resetToken

        val request = ResetPasswordRequest(rawToken, "newPass123")
        val result = authService.resetPassword(request)

        assertTrue(result)
        assertEquals("newEncoded", user.password)
        assertNotNull(resetToken.usedAt)
        verify { userRepository.save(user) }
        verify { passwordResetTokenRepository.save(resetToken) }
    }

    @Test
    fun `resetPassword should throw when token not found`() {
        val rawToken = "invalid"
        val tokenHash = authService.hashToken(rawToken)
        every { passwordResetTokenRepository.findByTokenHash(tokenHash) } returns Optional.empty()

        val request = ResetPasswordRequest(rawToken, "newPass")
        assertThrows(IllegalArgumentException::class.java) {
            authService.resetPassword(request)
        }
        verify { userRepository wasNot called }
    }

    @Test
    fun `resetPassword should throw when token already used`() {
        val user = User("john", "john@mail.com", "old", null, null, null).apply { id = 1L }
        val rawToken = "usedToken"
        val tokenHash = authService.hashToken(rawToken)
        val resetToken = PasswordResetToken(
            user = user,
            tokenHash = tokenHash,
            expiresAt = Instant.now().plusSeconds(3600),
            usedAt = Instant.now()
        )
        every { passwordResetTokenRepository.findByTokenHash(tokenHash) } returns Optional.of(resetToken)

        val request = ResetPasswordRequest(rawToken, "newPass")
        assertThrows(IllegalArgumentException::class.java) {
            authService.resetPassword(request)
        }
        verify { userRepository wasNot called }
    }

    @Test
    fun `resetPassword should throw when token expired`() {
        val user = User("john", "john@mail.com", "old", null, null, null).apply { id = 1L }
        val rawToken = "expiredToken"
        val tokenHash = authService.hashToken(rawToken)
        val resetToken = PasswordResetToken(
            user = user,
            tokenHash = tokenHash,
            expiresAt = Instant.now().minusSeconds(1),
            usedAt = null
        )
        every { passwordResetTokenRepository.findByTokenHash(tokenHash) } returns Optional.of(resetToken)

        val request = ResetPasswordRequest(rawToken, "newPass")
        assertThrows(IllegalArgumentException::class.java) {
            authService.resetPassword(request)
        }
        verify { userRepository wasNot called }
    }

    @Test
    fun `getCurrentUser should return user from security context`() {
        val user = User("john", "john@mail.com", "encoded", null, null, null).apply { id = 1L }
        val auth = UsernamePasswordAuthenticationToken(CustomUserDetails(user), null, emptyList())
        SecurityContextHolder.getContext().authentication = auth

        val result = authService.getCurrentUser()
        assertEquals(user, result)
    }

    @Test
    fun `getCurrentUser should return null when not authenticated`() {
        SecurityContextHolder.clearContext()
        assertNull(authService.getCurrentUser())
    }

    @Test
    fun `getCurrentUserResponse should return UserResponse when user authenticated`() {
        val user = User("john", "john@mail.com", "encoded", "John", "Doe", "123").apply {
            id = 1L
            roles.add(Role("USER").apply { id = 1L })
        }
        val auth = UsernamePasswordAuthenticationToken(CustomUserDetails(user), null, emptyList())
        SecurityContextHolder.getContext().authentication = auth

        val response = authService.getCurrentUserResponse()
        assertNotNull(response)
        assertEquals("john", response?.username)
        assertEquals("John", response?.firstName)
    }

    @Test
    fun `getCurrentUserResponse should return null when not authenticated`() {
        SecurityContextHolder.clearContext()
        assertNull(authService.getCurrentUserResponse())
    }

    private fun AuthService.hashToken(token: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val encodedHash = digest.digest(token.toByteArray(java.nio.charset.StandardCharsets.UTF_8))
        return java.util.HexFormat.of().formatHex(encodedHash)
    }
}