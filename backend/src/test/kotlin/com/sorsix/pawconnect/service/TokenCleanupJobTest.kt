package com.sorsix.pawconnect.service

import com.sorsix.pawconnect.repository.PasswordResetTokenRepository
import com.sorsix.pawconnect.repository.RefreshTokenRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TokenCleanupJobTest {
    private val refreshTokenRepository = mockk<RefreshTokenRepository>()
    private val passwordResetTokenRepository = mockk<PasswordResetTokenRepository>()
    private lateinit var job: TokenCleanupJob

    @BeforeEach
    fun setup() {
        job = TokenCleanupJob(refreshTokenRepository, passwordResetTokenRepository)
    }

    @Test
    fun `deletes expired or revoked refresh tokens and expired or used reset tokens`() {
        every { refreshTokenRepository.deleteExpiredOrRevoked(any()) } returns 3
        every { passwordResetTokenRepository.deleteExpiredOrUsed(any()) } returns 2

        job.cleanupExpiredTokens()

        verify { refreshTokenRepository.deleteExpiredOrRevoked(any()) }
        verify { passwordResetTokenRepository.deleteExpiredOrUsed(any()) }
    }

    @Test
    fun `does not fail when nothing needs cleanup`() {
        every { refreshTokenRepository.deleteExpiredOrRevoked(any()) } returns 0
        every { passwordResetTokenRepository.deleteExpiredOrUsed(any()) } returns 0

        job.cleanupExpiredTokens()

        verify { refreshTokenRepository.deleteExpiredOrRevoked(any()) }
        verify { passwordResetTokenRepository.deleteExpiredOrUsed(any()) }
    }
}
