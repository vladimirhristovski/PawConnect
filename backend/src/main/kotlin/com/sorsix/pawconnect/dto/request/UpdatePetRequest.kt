package com.sorsix.pawconnect.dto.request

import com.sorsix.pawconnect.domain.Gender
import com.sorsix.pawconnect.domain.Size
import java.math.BigDecimal
import java.time.LocalDate

data class UpdatePetRequest(
    val name: String? = null,
    val speciesCode: String? = null,
    val breedCodes: List<String>? = null,
    val gender: Gender? = null,
    val size: Size? = null,
    val age: Long? = null,
    val birthDate: LocalDate? = null,
    val weightKg: BigDecimal? = null,
    val description: String? = null,
    val goodWithKids: Boolean? = null,
    val goodWithOtherPets: Boolean? = null
)