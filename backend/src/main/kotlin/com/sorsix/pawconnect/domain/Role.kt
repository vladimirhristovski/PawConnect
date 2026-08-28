package com.sorsix.pawconnect.domain

import com.sorsix.pawconnect.domain.base.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "roles")
class Role(
    @Column(nullable = false, unique = true)
    val name: String,

    ) : BaseEntity()