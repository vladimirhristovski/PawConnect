package com.sorsix.pawconnect.domain.result

import com.sorsix.pawconnect.domain.AdoptionApplication
import org.springframework.data.domain.Page

sealed interface SubmitApplicationResult {
    data class Success(
        val application: AdoptionApplication,
    ) : SubmitApplicationResult

    data class NotFound(
        val message: String,
    ) : SubmitApplicationResult

    data class Conflict(
        val message: String,
    ) : SubmitApplicationResult

    data class Forbidden(
        val message: String,
    ) : SubmitApplicationResult
}

sealed interface ListApplicationsForListingResult {
    data class Success(
        val applications: Page<AdoptionApplication>,
    ) : ListApplicationsForListingResult

    data class NotFound(
        val message: String,
    ) : ListApplicationsForListingResult

    data class Forbidden(
        val message: String,
    ) : ListApplicationsForListingResult
}

sealed interface ReviewApplicationResult {
    data class Success(
        val application: AdoptionApplication,
    ) : ReviewApplicationResult

    data class NotFound(
        val message: String,
    ) : ReviewApplicationResult

    data class Conflict(
        val message: String,
    ) : ReviewApplicationResult

    data class Forbidden(
        val message: String,
    ) : ReviewApplicationResult
}

sealed interface WithdrawApplicationResult {
    data class Success(
        val application: AdoptionApplication,
    ) : WithdrawApplicationResult

    data class NotFound(
        val message: String,
    ) : WithdrawApplicationResult

    data class Conflict(
        val message: String,
    ) : WithdrawApplicationResult

    data class Forbidden(
        val message: String,
    ) : WithdrawApplicationResult
}
