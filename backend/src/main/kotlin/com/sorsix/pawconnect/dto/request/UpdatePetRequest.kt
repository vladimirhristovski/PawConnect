package com.sorsix.pawconnect.dto.request

import com.sorsix.pawconnect.domain.Gender
import com.sorsix.pawconnect.domain.Size
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size as SizeConstraint
import java.math.BigDecimal
import java.time.LocalDate

data class UpdatePetRequest(
    @field:SizeConstraint(max = 100) val name: String? = null,
    val speciesCode: String? = null,
    val breedCodes: List<String>? = null,
    val gender: Gender? = null,
    val size: Size? = null,
    @field:PositiveOrZero val age: Long? = null,
    val birthDate: LocalDate? = null,
    @field:PositiveOrZero val weightKg: BigDecimal? = null,
    @field:SizeConstraint(max = 5000) val description: String? = null,
    val goodWithKids: Boolean? = null,
    val goodWithOtherPets: Boolean? = null
)