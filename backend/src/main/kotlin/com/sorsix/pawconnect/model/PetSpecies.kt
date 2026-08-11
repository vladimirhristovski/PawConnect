package com.sorsix.pawconnect.model

import jakarta.persistence.*

@Entity
@Table(name = "pet_species")
class PetSpecies(
    @Column(nullable = false, unique = true)
    val code: String,

    @Column(nullable = false)
    val name: String,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}