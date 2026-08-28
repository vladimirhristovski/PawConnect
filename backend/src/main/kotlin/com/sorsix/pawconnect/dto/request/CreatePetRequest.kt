package com.sorsix.pawconnect.dto.request

import com.sorsix.pawconnect.domain.Gender
import com.sorsix.pawconnect.domain.Size
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.LocalDate

data class CreatePetRequest(
    @field:NotBlank val name: String,
    @field:NotBlank val speciesCode: String,
    val breedCodes: List<String> = emptyList(),
    @field:NotNull val gender: Gender,
    val size: Size? = null,
    val age: Long? = null,
    val birthDate: LocalDate? = null,
    val weightKg: BigDecimal? = null,
    val description: String? = null,
    val goodWithKids: Boolean = false,
    val goodWithOtherPets: Boolean = false,
    val photos: List<PetPhotoRequest> = emptyList()
)