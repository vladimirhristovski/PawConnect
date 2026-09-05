package com.sorsix.pawconnect.domain.result

import com.sorsix.pawconnect.dto.response.PetMatcherResponse

sealed interface PetMatcherResult {
    data class Success(
        val response: PetMatcherResponse,
    ) : PetMatcherResult

    data class ServiceUnavailable(
        val message: String,
    ) : PetMatcherResult
}
