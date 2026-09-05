package com.sorsix.pawconnect.domain

import com.sorsix.pawconnect.domain.base.BaseEntity
import jakarta.persistence.*

@Entity
@Table(name = "cities")
class City(
    @Column(nullable = false, unique = true)
    val code: String,
    @Column(nullable = false)
    val name: String,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_id")
    var country: Country? = null,
) : BaseEntity()
