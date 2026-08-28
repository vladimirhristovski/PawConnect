package com.sorsix.pawconnect.security

import com.sorsix.pawconnect.domain.PasswordResetToken
import com.sorsix.pawconnect.domain.RefreshToken
import com.sorsix.pawconnect.domain.User
import com.sorsix.pawconnect.repository.RefreshTokenRepository
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class JwtService(
    private val refreshTokenRepository: RefreshTokenRepository,
    @Value("\${app.jwt.secret}") private val secret: String,
    @Value("\${app.jwt.access-token-ttl}") private val accessTokenTtl: Long,
    @Value("\${app.jwt.refresh-token-ttl}") private val refreshTokenTtl: Long
) {

    private val key = Keys.hmacShaKeyFor(secret.toByteArray())

    fun generateAccessToken(userDetails: UserDetails): String {
        return Jwts.builder()
            .subject(userDetails.username)
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(Instant.now().plusMillis(accessTokenTtl)))
            .claim("jti", UUID.randomUUID().toString())
            .signWith(key)
            .compact()
    }

    fun validateAccessToken(token: String): Boolean {
        return try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun extractUsername(token: String): String? {
        return try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload.subject
        } catch (e: Exception) {
            null
        }
    }

    fun hashToken(token: String): String {
        return java.security.MessageDigest.getInstance("SHA-256").digest(token.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    fun verifyRefreshToken(token: String): RefreshToken? {
        val hashed = hashToken(token)
        val refreshToken = refreshTokenRepository.findByTokenHash(hashed).orElse(null)
        return if (refreshToken != null && refreshToken.revokedAt == null && refreshToken.expiresAt.isAfter(Instant.now())) refreshToken else null
    }

    fun revokeRefreshToken(token: String): Boolean {
        val hashed = hashToken(token)
        val rt = refreshTokenRepository.findByTokenHash(hashed).orElse(null)
        if (rt != null && rt.revokedAt == null) {
            rt.revokedAt = Instant.now()
            refreshTokenRepository.save(rt)
            return true
        }
        return false
    }

    fun generateRefreshToken(user: User): Pair<String, RefreshToken> {
        val raw = UUID.randomUUID().toString()
        val hashed = hashToken(raw)
        val entity = RefreshToken(
            user = user, tokenHash = hashed, expiresAt = Instant.now().plusMillis(refreshTokenTtl)
        )
        return raw to entity
    }

}