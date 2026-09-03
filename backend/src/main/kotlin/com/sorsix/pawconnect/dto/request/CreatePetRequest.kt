package com.sorsix.pawconnect.dto.request

import com.sorsix.pawconnect.domain.Gender
import com.sorsix.pawconnect.domain.Size
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size as SizeConstraint
import java.math.BigDecimal
import java.time.LocalDate

data class CreatePetRequest(
    @field:NotBlank @field:SizeConstraint(max = 100) val name: String,
    @field:NotBlank val speciesCode: String,
    val breedCodes: List<String> = emptyList(),
    @field:NotNull val gender: Gender,
    val size: Size? = null,
    @field:PositiveOrZero val age: Long? = null,
    val birthDate: LocalDate? = null,
    @field:PositiveOrZero val weightKg: BigDecimal? = null,
    @field:SizeConstraint(max = 5000) val description: String? = null,
    val goodWithKids: Boolean = false,
    val goodWithOtherPets: Boolean = false,
    @field:Valid val photos: List<PetPhotoRequest> = emptyList()
)