package com.sorsix.pawconnect.domain

import com.sorsix.pawconnect.domain.base.BaseEntity
import jakarta.persistence.*
import java.math.BigDecimal

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

    var latitude: BigDecimal? = null,

    var longitude: BigDecimal? = null,

    ) : BaseEntity()