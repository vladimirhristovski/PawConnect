package com.sorsix.pawconnect.repository

import com.sorsix.pawconnect.domain.PetSpecies
import org.springframework.data.jpa.repository.JpaRepository

interface PetSpeciesRepository : JpaRepository<PetSpecies, Long> {
    fun findByCode(code: String): PetSpecies?
}
