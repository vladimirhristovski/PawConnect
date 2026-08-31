package com.sorsix.pawconnect.service

import com.sorsix.pawconnect.dto.request.ApplicationDecision
import com.sorsix.pawconnect.dto.request.CreateApplicationRequest
import com.sorsix.pawconnect.domain.AdoptionApplication
import com.sorsix.pawconnect.domain.User
import com.sorsix.pawconnect.domain.result.ListApplicationsForListingResult
import com.sorsix.pawconnect.domain.result.ReviewApplicationResult
import com.sorsix.pawconnect.domain.result.SubmitApplicationResult
import com.sorsix.pawconnect.domain.result.WithdrawApplicationResult
import com.sorsix.pawconnect.repository.AdoptionApplicationRepository
import com.sorsix.pawconnect.repository.ApplicationStatusRepository
import com.sorsix.pawconnect.repository.ListingRepository
import com.sorsix.pawconnect.common.ApplicationStatusCodes
import com.sorsix.pawconnect.common.ListingStatusCodes
import com.sorsix.pawconnect.common.requireId
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
    fun submitApplication(listingId: Long, request: CreateApplicationRequest, currentUser: User): SubmitApplicationResult {
        val listing = listingRepository.findById(listingId).orElse(null)
            ?: return SubmitApplicationResult.NotFound("Listing not found: $listingId")

        if (listing.status.code != ListingStatusCodes.ACTIVE) {
            return SubmitApplicationResult.Conflict("Listing is not currently accepting applications")
        }

        if (listing.postedBy.id == currentUser.id) {
            return SubmitApplicationResult.Forbidden("You cannot apply to your own listing")
        }

        val existing = applicationRepository.findByListing_IdAndApplicant_IdAndStatus_CodeInAndDeletedAtIsNull(
            listingId, currentUser.requireId(), ApplicationStatusCodes.PENDING_STATUSES
        )
        if (existing.isNotEmpty()) {
            return SubmitApplicationResult.Conflict("You already have a pending application for this listing")
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
        val reloaded = applicationRepository.findByIdWithAllAssociations(saved.requireId())
            ?: throw IllegalStateException("Application not found after save")
        return SubmitApplicationResult.Success(reloaded)
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
    fun listApplicationsForListing(listingId: Long, currentUser: User, pageable: Pageable): ListApplicationsForListingResult {
        val listing = listingRepository.findById(listingId).orElse(null)
            ?: return ListApplicationsForListingResult.NotFound("Listing not found: $listingId")
        if (listing.postedBy.id != currentUser.id && !currentUser.isAdmin()) {
            return ListApplicationsForListingResult.Forbidden("You are not authorized to view applications for this listing")
        }
        val page = applicationRepository.findByListing_IdAndDeletedAtIsNull(listingId, pageable)
        return ListApplicationsForListingResult.Success(page)
    }

    @Transactional
    fun reviewApplication(applicationId: Long, decision: ApplicationDecision, currentUser: User): ReviewApplicationResult {
        val app = applicationRepository.findByIdWithAllAssociations(applicationId)
            ?: return ReviewApplicationResult.NotFound("Application not found: $applicationId")

        val listing = app.listing
        if (listing.postedBy.id != currentUser.id && !currentUser.isAdmin()) {
            return ReviewApplicationResult.Forbidden("You are not authorized to review this application")
        }

        if (app.status.code !in ApplicationStatusCodes.PENDING_STATUSES) {
            return ReviewApplicationResult.Conflict("Application is not in a pending state")
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
            return ReviewApplicationResult.Success(saved)
        } else {
            val rejectedStatus = applicationStatusRepository.findByCode(ApplicationStatusCodes.REJECTED)
                ?: throw IllegalStateException("REJECTED status not found")
            app.status = rejectedStatus

            val saved = applicationRepository.save(app)
            log.info("Application {} rejected by {}", saved.id, currentUser.id)
            return ReviewApplicationResult.Success(saved)
        }
    }

    @Transactional
    fun withdrawApplication(applicationId: Long, currentUser: User): WithdrawApplicationResult {
        val app = applicationRepository.findByIdWithAllAssociations(applicationId)
            ?: return WithdrawApplicationResult.NotFound("Application not found: $applicationId")

        if (app.applicant.id != currentUser.id) {
            return WithdrawApplicationResult.Forbidden("You are not the applicant")
        }

        if (app.status.code !in ApplicationStatusCodes.PENDING_STATUSES) {
            return WithdrawApplicationResult.Conflict("Application cannot be withdrawn in its current status")
        }

        val withdrawnStatus = applicationStatusRepository.findByCode(ApplicationStatusCodes.WITHDRAWN)
            ?: throw IllegalStateException("WITHDRAWN status not found")
        app.status = withdrawnStatus
        val saved = applicationRepository.save(app)
        log.info("Application {} withdrawn by applicant {}", saved.id, currentUser.id)
        return WithdrawApplicationResult.Success(saved)
    }
}
