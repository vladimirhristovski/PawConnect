package com.sorsix.pawconnect.dto.response

data class ExtendedTraits(
    val age: Int? = null,
    val dwelling: String? = null,
    val dwellingSize: String? = null,
    val hasYard: Boolean? = null,
    val hasKids: Boolean? = null,
    val kidsAges: String? = null,
    val maintenancePref: String? = null,
    val activityLevel: String? = null,
    val timeAvailability: String? = null,
    val experienceLevel: String? = null,
    val budget: String? = null,
    val allergies: Boolean? = null,
    val workSchedule: String? = null,
    val climate: String? = null,
    val otherPets: Boolean? = null,
)

data class PetMatch(
    val id: String,
    val name: String,
    val speciesCode: String,
    val breedCode: String? = null,
    val score: Int,
    val matchPercentage: Double,
    val reasons: List<String> = emptyList(),
    val concerns: List<String> = emptyList(),
    val notes: String? = null,
)

data class PetMatcherResponse(
    val understoodTraits: ExtendedTraits,
    val extractionMethod: String,
    val topMatch: PetMatch,
    val alternatives: List<PetMatch> = emptyList(),
    val confidence: Double,
)