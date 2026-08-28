package com.sorsix.pawconnect.repository

import com.sorsix.pawconnect.domain.Pet
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface PetRepository : JpaRepository<Pet, Long> {
    @Query(
        """
        SELECT p FROM Pet p
        JOIN FETCH p.species
        LEFT JOIN FETCH p.breeds
        LEFT JOIN FETCH p.photos
        WHERE p.id = :id
    """
    )
    fun findByIdWithAllAssociations(id: Long): Pet?

}