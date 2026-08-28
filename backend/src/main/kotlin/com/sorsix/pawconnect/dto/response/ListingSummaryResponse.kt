package com.sorsix.pawconnect.dto.response

import com.sorsix.pawconnect.model.Listing
import com.sorsix.pawconnect.util.requireId
import java.math.BigDecimal
import java.time.Instant

data class ListingSummaryResponse(
    val id: Long,
    val pet: PetSummaryResponse,
    val postedBy: String,
    val municipalityName: String,
    val statusCode: String,
    val statusName: String,
    val title: String?,
    val adoptionFee: BigDecimal,
    val expiresAt: Instant?,
    val createdAt: Instant
) {
    companion object {
        fun from(listing: Listing): ListingSummaryResponse {
            return ListingSummaryResponse(
                id = listing.requireId(),
                pet = PetSummaryResponse.from(listing.pet),
                postedBy = listing.postedBy.username,
                municipalityName = listing.municipality.name,
                statusCode = listing.status.code,
                statusName = listing.status.name,
                title = listing.title,
                adoptionFee = listing.adoptionFee,
                expiresAt = listing.expiresAt,
                createdAt = listing.createdAt
            )
        }
    }
}