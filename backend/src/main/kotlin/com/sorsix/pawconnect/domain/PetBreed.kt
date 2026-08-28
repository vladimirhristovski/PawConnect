package com.sorsix.pawconnect.domain

import com.sorsix.pawconnect.domain.base.BaseEntity
import jakarta.persistence.*

@Entity
@Table(name = "pet_breeds")
class PetBreed(
    @Column(nullable = false, unique = true)
    val code: String,

    @Column(nullable = false)
    val name: String,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "species_id", nullable = false)
    var species: PetSpecies

    ) : BaseEntity()