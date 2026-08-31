package com.sorsix.pawconnect.domain

import com.sorsix.pawconnect.domain.base.BaseEntity
import jakarta.persistence.*

@Entity
@Table(name = "business_photos")
class BusinessPhoto(
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(
        name = "business_id", nullable = false
    )
    var business: Business,

    @Column(nullable = false)
    var url: String,

    @Column(name = "is_primary", nullable = false)
    var isPrimary: Boolean = false,

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0,

    ) : BaseEntity()