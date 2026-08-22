package com.sorsix.pawconnect.dto.response

import com.sorsix.pawconnect.model.AdoptionApplication
import java.time.Instant

data class ApplicationResponse(
    val id: Long,
    val listingId: Long,
    val petName: String,
    val applicantId: Long,
    val applicantUsername: String,
    val statusCode: String,
    val statusName: String,
    val message: String?,
    val contactPhone: String?,
    val contactEmail: String?,
    val reviewedBy: String?, // username
    val reviewedAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        fun from(app: AdoptionApplication): ApplicationResponse {
            return ApplicationResponse(
                id = app.id!!,
                listingId = app.listing.id!!,
                petName = app.listing.pet.name,
                applicantId = app.applicant.id!!,
                applicantUsername = app.applicant.username,
                statusCode = app.status.code,
                statusName = app.status.name,
                message = app.message,
                contactPhone = app.contactPhone,
                contactEmail = app.contactEmail,
                reviewedBy = app.reviewedBy?.username,
                reviewedAt = app.reviewedAt,
                createdAt = app.createdAt,
                updatedAt = app.updatedAt
            )
        }
    }
}