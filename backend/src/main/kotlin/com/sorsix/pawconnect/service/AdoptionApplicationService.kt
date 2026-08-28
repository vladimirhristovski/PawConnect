package com.sorsix.pawconnect.service

import com.sorsix.pawconnect.dto.request.ApplicationDecision
import com.sorsix.pawconnect.dto.request.CreateApplicationRequest
import com.sorsix.pawconnect.exception.ConflictException
import com.sorsix.pawconnect.exception.ForbiddenOperationException
import com.sorsix.pawconnect.exception.ResourceNotFoundException
import com.sorsix.pawconnect.model.AdoptionApplication
import com.sorsix.pawconnect.model.User
import com.sorsix.pawconnect.repository.AdoptionApplicationRepository
import com.sorsix.pawconnect.repository.ApplicationStatusRepository
import com.sorsix.pawconnect.repository.ListingRepository
import com.sorsix.pawconnect.util.ApplicationStatusCodes
import com.sorsix.pawconnect.util.ListingStatusCodes
import com.sorsix.pawconnect.util.requireId
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class AdoptionApplicationService(
    private val applicationRepository: AdoptionApplicationRepository,
    private val listingRepository: ListingRepository,
    private val applicationStatusRepository: ApplicationStatusRepository,
    private val listingService: ListingService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun submitApplication(listingId: Long, request: CreateApplicationRequest, currentUser: User): AdoptionApplication {
        val listing = listingRepository.findById(listingId)
            .orElseThrow { ResourceNotFoundException("Listing not found: $listingId") }

        if (listing.status.code != ListingStatusCodes.ACTIVE) {
            throw ConflictException("Listing is not currently accepting applications")
        }

        if (listing.postedBy.id == currentUser.id) {
            throw ForbiddenOperationException("You cannot apply to your own listing")
        }

        val existing = applicationRepository.findByListing_IdAndApplicant_IdAndStatus_CodeInAndDeletedAtIsNull(
            listingId, currentUser.requireId(), ApplicationStatusCodes.PENDING_STATUSES
        )
        if (existing.isNotEmpty()) {
            throw ConflictException("You already have a pending application for this listing")
        }

        val submittedStatus = applicationStatusRepository.findByCode(ApplicationStatusCodes.SUBMITTED)
            ?: throw IllegalStateException("SUBMITTED status not found")

        val application = AdoptionApplication(
            listing = listing,
            applicant = currentUser,
            status = submittedStatus,
            message = request.message,
            contactPhone = request.contactPhone ?: currentUser.phone,
            contactEmail = request.contactEmail ?: currentUser.email
        )

        val saved = applicationRepository.save(application)
        log.info("Application {} submitted by user {} for listing {}", saved.id, currentUser.id, listingId)
        return applicationRepository.findByIdWithAllAssociations(saved.requireId())
            ?: throw IllegalStateException("Application not found after save")
    }

    @Transactional(readOnly = true)
    fun listMyApplications(currentUser: User, pageable: Pageable): Page<AdoptionApplication> {
        return applicationRepository.findByApplicant_IdAndDeletedAtIsNull(currentUser.requireId(), pageable)
    }

    @Transactional(readOnly = true)
    fun adminListApplications(statusCode: String?, pageable: Pageable): Page<AdoptionApplication> {
        return applicationRepository.findAllWithAssociations(statusCode, pageable)
    }

    @Transactional(readOnly = true)
    fun listApplicationsForListing(listingId: Long, currentUser: User, pageable: Pageable): Page<AdoptionApplication> {
        val listing = listingRepository.findById(listingId)
            .orElseThrow { ResourceNotFoundException("Listing not found: $listingId") }
        if (listing.postedBy.id != currentUser.id && !currentUser.isAdmin()) {
            throw ForbiddenOperationException("You are not authorized to view applications for this listing")
        }
        return applicationRepository.findByListing_IdAndDeletedAtIsNull(listingId, pageable)
    }

    @Transactional
    fun reviewApplication(applicationId: Long, decision: ApplicationDecision, currentUser: User): AdoptionApplication {
        val app = applicationRepository.findByIdWithAllAssociations(applicationId)
            ?: throw ResourceNotFoundException("Application not found: $applicationId")

        val listing = app.listing
        if (listing.postedBy.id != currentUser.id && !currentUser.isAdmin()) {
            throw ForbiddenOperationException("You are not authorized to review this application")
        }

        if (app.status.code !in ApplicationStatusCodes.PENDING_STATUSES) {
            throw ConflictException("Application is not in a pending state")
        }

        val now = Instant.now()
        app.reviewedBy = currentUser
        app.reviewedAt = now

        if (decision == ApplicationDecision.APPROVE) {
            val approvedStatus = applicationStatusRepository.findByCode(ApplicationStatusCodes.APPROVED)
                ?: throw IllegalStateException("APPROVED status not found")
            app.status = approvedStatus

            listingService.markAdopted(listing)

            val rejectedStatus = applicationStatusRepository.findByCode(ApplicationStatusCodes.REJECTED)
                ?: throw IllegalStateException("REJECTED status not found")
            val otherPending = applicationRepository.findByListing_IdAndStatus_CodeInAndDeletedAtIsNull(
                listing.requireId(), ApplicationStatusCodes.PENDING_STATUSES
            ).filter { it.id != app.id }
            otherPending.forEach { other ->
                other.status = rejectedStatus
                other.reviewedBy = currentUser
                other.reviewedAt = now
            }
            applicationRepository.saveAll(otherPending)

            val saved = applicationRepository.save(app)
            log.info(
                "Application {} approved by {}; listing {} marked adopted, {} other application(s) auto-rejected",
                saved.id,
                currentUser.id,
                listing.id,
                otherPending.size
            )
            return saved
        } else {
            val rejectedStatus = applicationStatusRepository.findByCode(ApplicationStatusCodes.REJECTED)
                ?: throw IllegalStateException("REJECTED status not found")
            app.status = rejectedStatus

            val saved = applicationRepository.save(app)
            log.info("Application {} rejected by {}", saved.id, currentUser.id)
            return saved
        }
    }

    @Transactional
    fun withdrawApplication(applicationId: Long, currentUser: User): AdoptionApplication {
        val app = applicationRepository.findByIdWithAllAssociations(applicationId)
            ?: throw ResourceNotFoundException("Application not found: $applicationId")

        if (app.applicant.id != currentUser.id) {
            throw ForbiddenOperationException("You are not the applicant")
        }

        if (app.status.code !in ApplicationStatusCodes.PENDING_STATUSES) {
            throw ConflictException("Application cannot be withdrawn in its current status")
        }

        val withdrawnStatus = applicationStatusRepository.findByCode(ApplicationStatusCodes.WITHDRAWN)
            ?: throw IllegalStateException("WITHDRAWN status not found")
        app.status = withdrawnStatus
        val saved = applicationRepository.save(app)
        log.info("Application {} withdrawn by applicant {}", saved.id, currentUser.id)
        return saved
    }
}