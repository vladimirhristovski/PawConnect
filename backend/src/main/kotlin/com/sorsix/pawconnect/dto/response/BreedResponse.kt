package com.sorsix.pawconnect.dto.response

import com.sorsix.pawconnect.domain.PetBreed

data class BreedResponse(
    val code: String,
    val name: String,
    val speciesCode: String,
) {
    companion object {
        fun from(breed: PetBreed) = BreedResponse(breed.code, breed.name, breed.species.code)
    }
}
