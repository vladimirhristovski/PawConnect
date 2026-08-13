package com.sorsix.pawconnect.model

import com.sorsix.pawconnect.model.base.BaseEntity
import jakarta.persistence.*

@Entity
@Table(name = "municipalities")
class Municipality(
    @Column(nullable = false, unique = true)
    val code: String,

    @Column(nullable = false)
    val name: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id")
    var city: City? = null,

    ) : BaseEntity()