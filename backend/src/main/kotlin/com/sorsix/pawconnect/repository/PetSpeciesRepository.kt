package com.sorsix.pawconnect.repository

import com.sorsix.pawconnect.model.PetSpecies
import org.springframework.data.jpa.repository.JpaRepository

interface PetSpeciesRepository : JpaRepository<PetSpecies, Long> {
    fun findByCode(code: String): PetSpecies?
}