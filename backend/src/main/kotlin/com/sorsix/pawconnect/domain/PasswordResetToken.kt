package com.sorsix.pawconnect.domain

import com.sorsix.pawconnect.domain.base.BaseEntity
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "password_reset_tokens")
class PasswordResetToken(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @Column(name = "token_hash", nullable = false, unique = true)
    var tokenHash: String,

    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant,

    @Column(name = "used_at")
    var usedAt: Instant? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()
) : BaseEntity()