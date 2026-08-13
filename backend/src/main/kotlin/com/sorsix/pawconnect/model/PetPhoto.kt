package com.sorsix.pawconnect.model

import com.sorsix.pawconnect.model.base.BaseEntity
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
    var isPrimary: Boolean = false,

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0,

    ) : BaseEntity()