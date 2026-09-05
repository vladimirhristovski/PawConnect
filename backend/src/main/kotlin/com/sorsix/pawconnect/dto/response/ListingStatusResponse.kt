package com.sorsix.pawconnect.dto.response

import com.sorsix.pawconnect.domain.ListingStatus

data class ListingStatusResponse(
    val code: String, val name: String
) {
    companion object {
        fun from(status: ListingStatus) = ListingStatusResponse(status.code, status.name)
    }
}
