package com.sorsix.pawconnect.repository

import com.sorsix.pawconnect.domain.PasswordResetToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import jakarta.transaction.Transactional
import java.time.Instant
import java.util.*

interface PasswordResetTokenRepository : JpaRepository<PasswordResetToken, Long> {
    fun findByTokenHash(tokenHash: String): Optional<PasswordResetToken>

    @Modifying
    @Transactional
    @Query("UPDATE PasswordResetToken t SET t.usedAt = :now WHERE t.user.id = :userId AND t.usedAt IS NULL")
    fun revokeAllUnusedTokensForUser(@Param("userId") userId: Long, @Param("now") now: Instant): Int

    @Modifying
    @Transactional
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiresAt < :now OR t.usedAt IS NOT NULL")
    fun deleteExpiredOrUsed(@Param("now") now: Instant): Int
}