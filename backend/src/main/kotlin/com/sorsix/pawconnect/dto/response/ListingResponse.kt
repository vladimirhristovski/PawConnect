package com.sorsix.pawconnect.dto.response

import com.sorsix.pawconnect.model.Listing
import java.math.BigDecimal
import java.time.Instant

data class ListingResponse(
    val id: Long,
    val pet: PetResponse,
    val postedBy: String,
    val municipalityCode: String,
    val municipalityName: String,
    val statusCode: String,
    val statusName: String,
    val business: BusinessResponse?,
    val title: String?,
    val description: String?,
    val adoptionFee: BigDecimal,
    val latitude: BigDecimal?,
    val longitude: BigDecimal?,
    val expiresAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        fun from(listing: Listing): ListingResponse {
            return ListingResponse(
                id = listing.id!!,
                pet = PetResponse.from(listing.pet),
                postedBy = listing.postedBy.username,
                municipalityCode = listing.municipality.code,
                municipalityName = listing.municipality.name,
                statusCode = listing.status.code,
                statusName = listing.status.name,
                business = listing.business?.let { BusinessResponse.from(it) },
                title = listing.title,
                description = listing.description,
                adoptionFee = listing.adoptionFee,
                latitude = listing.latitude,
                longitude = listing.longitude,
                expiresAt = listing.expiresAt,
                createdAt = listing.createdAt,
                updatedAt = listing.updatedAt
            )
        }
    }
}