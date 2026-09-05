package com.sorsix.pawconnect.api

import com.sorsix.pawconnect.common.problemResponse
import com.sorsix.pawconnect.domain.result.ListApplicationsForListingResult
import com.sorsix.pawconnect.domain.result.ReviewApplicationResult
import com.sorsix.pawconnect.domain.result.SubmitApplicationResult
import com.sorsix.pawconnect.domain.result.WithdrawApplicationResult
import com.sorsix.pawconnect.dto.request.ApplicationDecision
import com.sorsix.pawconnect.dto.request.CreateApplicationRequest
import com.sorsix.pawconnect.dto.response.ApplicationResponse
import com.sorsix.pawconnect.service.AdoptionApplicationService
import com.sorsix.pawconnect.service.AuthService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
class AdoptionApplicationController(
    private val applicationService: AdoptionApplicationService,
    private val authService: AuthService,
) {
    @PostMapping("/api/listings/{listingId}/applications")
    fun submitApplication(
        @PathVariable listingId: Long,
        @Valid @RequestBody request: CreateApplicationRequest,
    ): ResponseEntity<*> {
        val currentUser = authService.requireCurrentUser()
        return when (val result = applicationService.submitApplication(listingId, request, currentUser)) {
            is SubmitApplicationResult.Success ->
                ResponseEntity.status(HttpStatus.CREATED).body(ApplicationResponse.from(result.application))
            is SubmitApplicationResult.NotFound -> problemResponse(HttpStatus.NOT_FOUND, result.message)
            is SubmitApplicationResult.Conflict -> problemResponse(HttpStatus.CONFLICT, result.message)
            is SubmitApplicationResult.Forbidden -> problemResponse(HttpStatus.FORBIDDEN, result.message)
        }
    }

    @GetMapping("/api/listings/{listingId}/applications")
    fun listApplicationsForListing(
        @PathVariable listingId: Long,
        pageable: Pageable,
    ): ResponseEntity<*> {
        val currentUser = authService.requireCurrentUser()
        return when (val result = applicationService.listApplicationsForListing(listingId, currentUser, pageable)) {
            is ListApplicationsForListingResult.Success ->
                ResponseEntity.ok(result.applications.map { ApplicationResponse.from(it) })
            is ListApplicationsForListingResult.NotFound -> problemResponse(HttpStatus.NOT_FOUND, result.message)
            is ListApplicationsForListingResult.Forbidden -> problemResponse(HttpStatus.FORBIDDEN, result.message)
        }
    }

    @GetMapping("/api/applications/mine")
    fun listMyApplications(pageable: Pageable): Page<ApplicationResponse> {
        val currentUser = authService.requireCurrentUser()
        val page = applicationService.listMyApplications(currentUser, pageable)
        return page.map { ApplicationResponse.from(it) }
    }

    @PatchMapping("/api/applications/{id}/review")
    fun reviewApplication(
        @PathVariable id: Long,
        @RequestParam decision: ApplicationDecision,
    ): ResponseEntity<*> {
        val currentUser = authService.requireCurrentUser()
        return when (val result = applicationService.reviewApplication(id, decision, currentUser)) {
            is ReviewApplicationResult.Success -> ResponseEntity.ok(ApplicationResponse.from(result.application))
            is ReviewApplicationResult.NotFound -> problemResponse(HttpStatus.NOT_FOUND, result.message)
            is ReviewApplicationResult.Conflict -> problemResponse(HttpStatus.CONFLICT, result.message)
            is ReviewApplicationResult.Forbidden -> problemResponse(HttpStatus.FORBIDDEN, result.message)
        }
    }

    @PostMapping("/api/applications/{id}/withdraw")
    fun withdrawApplication(
        @PathVariable id: Long,
    ): ResponseEntity<*> {
        val currentUser = authService.requireCurrentUser()
        return when (val result = applicationService.withdrawApplication(id, currentUser)) {
            is WithdrawApplicationResult.Success -> ResponseEntity.ok(ApplicationResponse.from(result.application))
            is WithdrawApplicationResult.NotFound -> problemResponse(HttpStatus.NOT_FOUND, result.message)
            is WithdrawApplicationResult.Conflict -> problemResponse(HttpStatus.CONFLICT, result.message)
            is WithdrawApplicationResult.Forbidden -> problemResponse(HttpStatus.FORBIDDEN, result.message)
        }
    }
}
