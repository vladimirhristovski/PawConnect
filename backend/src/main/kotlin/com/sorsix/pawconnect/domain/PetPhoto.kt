package com.sorsix.pawconnect.domain

import com.sorsix.pawconnect.domain.base.BaseEntity
import jakarta.persistence.*

@Entity
@Table(name = "pet_photos")
class PetPhoto(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pet_id", nullable = false)
    var pet: Pet,
    @Column(nullable = false)
    var url: String,
    @Column(name = "is_primary", nullable = false)
    override var isPrimary: Boolean = false,
    @Column(name = "display_order", nullable = false)
    override var displayOrder: Int = 0,
) : BaseEntity(),
    Photo
