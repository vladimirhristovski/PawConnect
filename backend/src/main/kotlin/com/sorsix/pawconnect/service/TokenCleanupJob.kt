package com.sorsix.pawconnect.service

import com.sorsix.pawconnect.repository.PasswordResetTokenRepository
import com.sorsix.pawconnect.repository.RefreshTokenRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class TokenCleanupJob(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val passwordResetTokenRepository: PasswordResetTokenRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = "\${app.tokens.cleanup-interval}")
    fun cleanupExpiredTokens() {
        val now = Instant.now()
        val refreshTokensDeleted = refreshTokenRepository.deleteExpiredOrRevoked(now)
        val resetTokensDeleted = passwordResetTokenRepository.deleteExpiredOrUsed(now)

        if (refreshTokensDeleted > 0 || resetTokensDeleted > 0) {
            log.info(
                "Token cleanup: {} refresh token(s), {} password reset token(s) deleted",
                refreshTokensDeleted,
                resetTokensDeleted,
            )
        }
    }
}
