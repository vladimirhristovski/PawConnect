package com.sorsix.pawconnect.domain.base

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import java.time.Instant

@MappedSuperclass
abstract class SoftDeletableEntity : AuditableEntity() {
    @Column(name = "deleted_at")
    var deletedAt: Instant? = null
}
