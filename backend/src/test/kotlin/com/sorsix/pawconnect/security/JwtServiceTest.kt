package com.sorsix.pawconnect.security

import com.sorsix.pawconnect.domain.RefreshToken
import com.sorsix.pawconnect.domain.User
import com.sorsix.pawconnect.repository.RefreshTokenRepository
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.core.userdetails.UserDetails
import java.time.Instant
import java.util.*

class JwtServiceTest {

    private lateinit var refreshTokenRepository: RefreshTokenRepository
    private lateinit var jwtService: JwtService

    private val secret = "01234567890123456789012345678901" // 32 chars for HS256
    private val accessTtl = 900_000L // 15 min
    private val refreshTtl = 2_592_000_000L // 30 days

    @BeforeEach
    fun setup() {
        refreshTokenRepository = mockk()
        jwtService = JwtService(refreshTokenRepository, secret, accessTtl, refreshTtl)
    }

    @Test
    fun `generateAccessToken should produce valid token with correct subject`() {
        val userDetails = mockk<UserDetails> {
            every { username } returns "john"
        }
        val token = jwtService.generateAccessToken(userDetails)
        assertNotNull(token)

        // Validate
        val username = jwtService.extractUsername(token)
        assertEquals("john", username)
        assertTrue(jwtService.validateAccessToken(token))
    }

    @Test
    fun `validateAccessToken should return false for tampered token`() {
        val userDetails = mockk<UserDetails> { every { username } returns "john" }
        val token = jwtService.generateAccessToken(userDetails)
        val parts = token.split(".")
        val payload = parts[1]
        val middleIndex = payload.length / 2
        val tamperedChar = if (payload[middleIndex] == 'A') 'B' else 'A'
        val tamperedPayload = payload.substring(0, middleIndex) + tamperedChar + payload.substring(middleIndex + 1)
        val tampered = "${parts[0]}.$tamperedPayload.${parts[2]}"
        assertFalse(jwtService.validateAccessToken(tampered))
    }

    @Test
    fun `extractUsername should return null for invalid token`() {
        assertNull(jwtService.extractUsername("invalid.token.here"))
    }

    @Test
    fun `hashToken should produce SHA-256 digest`() {
        val token = "some-random-string"
        val hash = jwtService.hashToken(token)
        // SHA-256 is 64 hex chars
        assertEquals(64, hash.length)
        // Should be deterministic
        assertEquals(jwtService.hashToken(token), hash)
    }

    @Test
    fun `verifyRefreshToken should return token if valid`() {
        val raw = "raw_refresh"
        val hash = jwtService.hashToken(raw)
        val user = User("john", "john@mail.com", "encoded", null, null, null).apply { id = 1L }
        val refreshToken = RefreshToken(
            user = user,
            tokenHash = hash,
            expiresAt = Instant.now().plusSeconds(3600),
            revokedAt = null
        )
        every { refreshTokenRepository.findByTokenHash(hash) } returns Optional.of(refreshToken)

        val result = jwtService.verifyRefreshToken(raw)
        assertNotNull(result)
        assertEquals(refreshToken, result)
    }

    @Test
    fun `verifyRefreshToken should return null if token expired`() {
        val raw = "raw_refresh"
        val hash = jwtService.hashToken(raw)
        val user = User("john", "john@mail.com", "encoded", null, null, null).apply { id = 1L }
        val refreshToken = RefreshToken(
            user = user,
            tokenHash = hash,
            expiresAt = Instant.now().minusSeconds(1),
            revokedAt = null
        )
        every { refreshTokenRepository.findByTokenHash(hash) } returns Optional.of(refreshToken)

        val result = jwtService.verifyRefreshToken(raw)
        assertNull(result)
    }

    @Test
    fun `verifyRefreshToken should return null if revoked`() {
        val raw = "raw_refresh"
        val hash = jwtService.hashToken(raw)
        val user = User("john", "john@mail.com", "encoded", null, null, null).apply { id = 1L }
        val refreshToken = RefreshToken(
            user = user,
            tokenHash = hash,
            expiresAt = Instant.now().plusSeconds(3600),
            revokedAt = Instant.now()
        )
        every { refreshTokenRepository.findByTokenHash(hash) } returns Optional.of(refreshToken)

        val result = jwtService.verifyRefreshToken(raw)
        assertNull(result)
    }

    @Test
    fun `revokeRefreshToken should set revoked_at and return true`() {
        val raw = "raw_refresh"
        val hash = jwtService.hashToken(raw)
        val user = User("john", "john@mail.com", "encoded", null, null, null).apply { id = 1L }
        val refreshToken = RefreshToken(
            user = user,
            tokenHash = hash,
            expiresAt = Instant.now().plusSeconds(3600),
            revokedAt = null
        )
        every { refreshTokenRepository.findByTokenHash(hash) } returns Optional.of(refreshToken)
        every { refreshTokenRepository.save(refreshToken) } returns refreshToken

        val result = jwtService.revokeRefreshToken(raw)
        assertTrue(result)
        assertNotNull(refreshToken.revokedAt)
        verify { refreshTokenRepository.save(refreshToken) }
    }

    @Test
    fun `revokeRefreshToken should return false if token not found`() {
        every { refreshTokenRepository.findByTokenHash(any()) } returns Optional.empty()
        val result = jwtService.revokeRefreshToken("unknown")
        assertFalse(result)
    }

    @Test
    fun `generateRefreshToken should return raw and entity with hashed token`() {
        val user = User("john", "john@mail.com", "encoded", null, null, null).apply { id = 1L }
        val (raw, entity) = jwtService.generateRefreshToken(user)

        assertNotNull(raw)
        assertEquals(36, raw.length) // UUID
        val expectedHash = jwtService.hashToken(raw)
        assertEquals(expectedHash, entity.tokenHash)
        assertNotNull(entity.expiresAt)
        assertTrue(entity.expiresAt.isAfter(Instant.now()))
    }
}