package com.sorsix.pawconnect.model

import com.sorsix.pawconnect.model.enums.Gender
import com.sorsix.pawconnect.model.enums.Size
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDate
import java.time.LocalDateTime

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

    @Column(name = "birth_date")
    var birthDate: LocalDate? = null,

    @Column(name = "good_with_kids", nullable = false)
    var goodWithKids: Boolean = false,

    @Column(name = "good_with_other_pets", nullable = false)
    var goodWithOtherPets: Boolean = false,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "pet_breed_links",
        joinColumns = [JoinColumn(name = "pet_id")],
        inverseJoinColumns = [JoinColumn(name = "breed_id")]
    )
    var breeds: MutableSet<PetBreed> = mutableSetOf()

    @OneToMany(mappedBy = "pet", cascade = [CascadeType.ALL], orphanRemoval = true)
    var photos: MutableList<PetPhoto> = mutableListOf()

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
}