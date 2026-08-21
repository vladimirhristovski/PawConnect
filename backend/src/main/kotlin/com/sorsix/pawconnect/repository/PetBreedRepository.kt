package com.sorsix.pawconnect.repository

import com.sorsix.pawconnect.model.PetBreed
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface PetBreedRepository : JpaRepository<PetBreed, Long> {
    fun findByCode(code: String): PetBreed?
    fun findByCodeIn(codes: Collection<String>): List<PetBreed>
    @Query("""
        SELECT b FROM PetBreed b
        JOIN FETCH b.species
        WHERE (:speciesCode IS NULL OR b.species.code = :speciesCode)
    """)
    fun findWithSpeciesBySpeciesCode(@Param("speciesCode") speciesCode: String?): List<PetBreed>
}