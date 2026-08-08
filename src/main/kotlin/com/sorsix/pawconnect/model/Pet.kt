package com.sorsix.pawconnect.model

import com.sorsix.pawconnect.model.enums.Gender
import com.sorsix.pawconnect.model.enums.PetStatus
import com.sorsix.pawconnect.model.enums.Size
import com.sorsix.pawconnect.model.enums.Species
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var species: Species,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var gender: Gender,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "municipality_id", nullable = false)
    var municipality: Municipality,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "posted_by", nullable = false)
    var postedBy: User,

    var breed: String? = null,

    @Enumerated(EnumType.STRING)
    var size: Size? = null,

    @Column(name = "birth_date")
    var birthDate: LocalDate? = null,

    @Column(name = "good_with_kids", nullable = false)
    var goodWithKids: Boolean = false,

    @Column(name = "good_with_other_pets", nullable = false)
    var goodWithOtherPets: Boolean = false,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Column(name = "photo_url")
    var photoUrl: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: PetStatus = PetStatus.AVAILABLE,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime? = null

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime? = null
}