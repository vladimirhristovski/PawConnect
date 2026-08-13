package com.sorsix.pawconnect.model

import com.sorsix.pawconnect.model.base.BaseEntity
import com.sorsix.pawconnect.model.enums.Gender
import com.sorsix.pawconnect.model.enums.Size
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@Table(name = "pets")
class Pet(
    @Column(nullable = false)
    var name: String,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "species_id", nullable = false)
    var species: PetSpecies,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var gender: Gender,

    @Enumerated(EnumType.STRING)
    var size: Size? = null,

    var age: Long? = null,

    @Column(name = "birth_date")
    var birthDate: LocalDate? = null,

    @Column(name = "weight_kg")
    var weightKg: BigDecimal? = null,

    @Column(columnDefinition = "text")
    var description: String? = null,

    @Column(name = "good_with_kids", nullable = false)
    var goodWithKids: Boolean = false,

    @Column(name = "good_with_other_pets", nullable = false)
    var goodWithOtherPets: Boolean = false,

    ) : BaseEntity() {

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "pet_breed_links",
        joinColumns = [JoinColumn(name = "pet_id")],
        inverseJoinColumns = [JoinColumn(name = "breed_id")]
    )
    var breeds: MutableSet<PetBreed> = mutableSetOf()

    @OneToMany(mappedBy = "pet", cascade = [CascadeType.ALL], orphanRemoval = true)
    var photos: MutableList<PetPhoto> = mutableListOf()
}