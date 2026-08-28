package com.sorsix.pawconnect.api

import com.sorsix.pawconnect.dto.request.ApplicationDecision
import com.sorsix.pawconnect.dto.request.CreateApplicationRequest
import com.sorsix.pawconnect.dto.response.ApplicationResponse
import com.sorsix.pawconnect.service.AdoptionApplicationService
import com.sorsix.pawconnect.service.AuthService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
class AdoptionApplicationController(
    private val applicationService: AdoptionApplicationService, private val authService: AuthService
) {

    @PostMapping("/api/listings/{listingId}/applications")
    @ResponseStatus(HttpStatus.CREATED)
    fun submitApplication(
        @PathVariable listingId: Long, @Valid @RequestBody request: CreateApplicationRequest
    ): ApplicationResponse {
        val currentUser = authService.requireCurrentUser()
        val app = applicationService.submitApplication(listingId, request, currentUser)
        return ApplicationResponse.from(app)
    }

    @GetMapping("/api/listings/{listingId}/applications")
    fun listApplicationsForListing(
        @PathVariable listingId: Long, @PageableDefault(size = 20) pageable: Pageable
    ): Page<ApplicationResponse> {
        val currentUser = authService.requireCurrentUser()
        val page = applicationService.listApplicationsForListing(listingId, currentUser, pageable)
        return page.map { ApplicationResponse.from(it) }
    }

    @GetMapping("/api/applications/mine")
    fun listMyApplications(@PageableDefault(size = 20) pageable: Pageable): Page<ApplicationResponse> {
        val currentUser = authService.requireCurrentUser()
        val page = applicationService.listMyApplications(currentUser, pageable)
        return page.map { ApplicationResponse.from(it) }
    }

    @PatchMapping("/api/applications/{id}/review")
    fun reviewApplication(
        @PathVariable id: Long, @RequestParam decision: ApplicationDecision
    ): ApplicationResponse {
        val currentUser = authService.requireCurrentUser()
        val app = applicationService.reviewApplication(id, decision, currentUser)
        return ApplicationResponse.from(app)
    }

    @PostMapping("/api/applications/{id}/withdraw")
    fun withdrawApplication(@PathVariable id: Long): ApplicationResponse {
        val currentUser = authService.requireCurrentUser()
        val app = applicationService.withdrawApplication(id, currentUser)
        return ApplicationResponse.from(app)
    }
}