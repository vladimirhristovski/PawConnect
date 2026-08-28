package com.sorsix.pawconnect.dto.response

import com.sorsix.pawconnect.domain.Pet
import com.sorsix.pawconnect.domain.Gender
import com.sorsix.pawconnect.domain.Size
import com.sorsix.pawconnect.common.requireId
import java.math.BigDecimal
import java.time.LocalDate

data class PetResponse(
    val id: Long,
    val name: String,
    val speciesCode: String,
    val speciesName: String,
    val breeds: List<BreedResponse>,
    val gender: Gender,
    val size: Size?,
    val age: Long?,
    val birthDate: LocalDate?,
    val weightKg: BigDecimal?,
    val description: String?,
    val goodWithKids: Boolean,
    val goodWithOtherPets: Boolean,
    val photos: List<PetPhotoResponse>
) {
    companion object {
        fun from(pet: Pet): PetResponse {
            return PetResponse(
                id = pet.requireId(),
                name = pet.name,
                speciesCode = pet.species.code,
                speciesName = pet.species.name,
                breeds = pet.breeds.map { BreedResponse.from(it) },
                gender = pet.gender,
                size = pet.size,
                age = pet.age,
                birthDate = pet.birthDate,
                weightKg = pet.weightKg,
                description = pet.description,
                goodWithKids = pet.goodWithKids,
                goodWithOtherPets = pet.goodWithOtherPets,
                photos = pet.photos.map { PetPhotoResponse.from(it) }.sortedBy { it.displayOrder })
        }
    }
}