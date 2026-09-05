package com.sorsix.pawconnect.dto.response

import com.sorsix.pawconnect.common.requireId
import com.sorsix.pawconnect.domain.Gender
import com.sorsix.pawconnect.domain.Pet
import com.sorsix.pawconnect.domain.Size

data class PetSummaryResponse(
    val id: Long,
    val name: String,
    val speciesName: String,
    val gender: Gender,
    val size: Size?,
    val primaryPhotoUrl: String?,
) {
    companion object {
        fun from(pet: Pet): PetSummaryResponse {
            val primaryPhoto =
                pet.photos.sortedBy { it.displayOrder }.firstOrNull { it.isPrimary }
                    ?: pet.photos.minByOrNull { it.displayOrder }
            return PetSummaryResponse(
                id = pet.requireId(),
                name = pet.name,
                speciesName = pet.species.name,
                gender = pet.gender,
                size = pet.size,
                primaryPhotoUrl = primaryPhoto?.url,
            )
        }
    }
}
