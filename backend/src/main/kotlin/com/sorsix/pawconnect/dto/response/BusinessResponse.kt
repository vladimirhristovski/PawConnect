package com.sorsix.pawconnect.dto.response

import com.sorsix.pawconnect.domain.Business
import com.sorsix.pawconnect.common.requireId
import java.math.BigDecimal

data class BusinessResponse(
    val id: Long,
    val typeCode: String,
    val typeName: String,
    val name: String,
    val description: String?,
    val phone: String,
    val email: String?,
    val address: String,
    val municipalityCode: String,
    val municipalityName: String,
    val ownerUsername: String?,
    val latitude: BigDecimal?,
    val longitude: BigDecimal?
) {
    companion object {
        fun from(business: Business): BusinessResponse {
            return BusinessResponse(
                id = business.requireId(),
                typeCode = business.type.code,
                typeName = business.type.name,
                name = business.name,
                description = business.description,
                phone = business.phone,
                email = business.email,
                address = business.address,
                municipalityCode = business.municipality.code,
                municipalityName = business.municipality.name,
                ownerUsername = business.owner?.username,
                latitude = business.latitude,
                longitude = business.longitude
            )
        }
    }
}