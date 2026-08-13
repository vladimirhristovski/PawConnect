package com.sorsix.pawconnect.model

import com.sorsix.pawconnect.model.base.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "pet_species")
class PetSpecies(
    @Column(nullable = false, unique = true)
    val code: String,

    @Column(nullable = false)
    val name: String,

    ) : BaseEntity()