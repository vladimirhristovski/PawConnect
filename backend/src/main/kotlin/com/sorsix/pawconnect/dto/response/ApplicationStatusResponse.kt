package com.sorsix.pawconnect.dto.response

import com.sorsix.pawconnect.domain.ApplicationStatus

data class ApplicationStatusResponse(
    val code: String,
    val name: String,
) {
    companion object {
        fun from(status: ApplicationStatus) = ApplicationStatusResponse(status.code, status.name)
    }
}
