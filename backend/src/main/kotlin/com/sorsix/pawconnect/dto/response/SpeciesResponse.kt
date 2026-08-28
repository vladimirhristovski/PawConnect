package com.sorsix.pawconnect.dto.response

import com.sorsix.pawconnect.domain.PetSpecies

data class SpeciesResponse(
    val code: String, val name: String
) {
    companion object {
        fun from(species: PetSpecies) = SpeciesResponse(species.code, species.name)
    }
}