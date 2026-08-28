package com.sorsix.pawconnect.repository

import com.sorsix.pawconnect.domain.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import jakarta.transaction.Transactional
import java.time.Instant
import java.util.Optional

interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {
    fun findByTokenHash(tokenHash: String): Optional<RefreshToken>

    @Modifying
    @Transactional
    @Query("UPDATE RefreshToken rt SET rt.revokedAt = :now WHERE rt.user.id = :userId AND rt.revokedAt IS NULL")
    fun revokeAllUserTokens(@Param("userId") userId: Long, @Param("now") now: Instant): Int

    @Modifying
    @Transactional
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now OR rt.revokedAt IS NOT NULL")
    fun deleteExpiredOrRevoked(@Param("now") now: Instant): Int
}