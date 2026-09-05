package com.sorsix.pawconnect.dto.response

import com.sorsix.pawconnect.domain.BusinessType

data class BusinessTypeResponse(
    val code: String,
    val name: String,
) {
    companion object {
        fun from(type: BusinessType) = BusinessTypeResponse(type.code, type.name)
    }
}
