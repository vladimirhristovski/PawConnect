package com.sorsix.pawconnect.domain

import com.sorsix.pawconnect.domain.base.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "application_statuses")
class ApplicationStatus(
    @Column(nullable = false, unique = true)
    val code: String,

    @Column(nullable = false)
    val name: String,

    ) : BaseEntity()