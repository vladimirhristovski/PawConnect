package com.sorsix.pawconnect.model

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
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}