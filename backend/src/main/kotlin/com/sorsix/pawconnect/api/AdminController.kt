package com.sorsix.pawconnect.api

import com.sorsix.pawconnect.dto.request.UpdateUserStatusRequest
import com.sorsix.pawconnect.dto.response.ApplicationResponse
import com.sorsix.pawconnect.dto.response.ListingResponse
import com.sorsix.pawconnect.dto.response.UserResponse
import com.sorsix.pawconnect.exception.ForbiddenOperationException
import com.sorsix.pawconnect.service.AdoptionApplicationService
import com.sorsix.pawconnect.service.AuthService
import com.sorsix.pawconnect.service.ListingService
import com.sorsix.pawconnect.service.UserService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin")
class AdminController(
    private val userService: UserService,
    private val listingService: ListingService,
    private val applicationService: AdoptionApplicationService,
    private val authService: AuthService
) {

    @GetMapping("/users")
    fun searchUsers(
        @RequestParam(required = false) active: Boolean?,
        @RequestParam(required = false) role: String?,
        @PageableDefault(size = 20) pageable: Pageable
    ): Page<UserResponse> {
        requireAdmin()
        return userService.searchUsers(active, role, pageable)
    }

    @PatchMapping("/users/{id}/status")
    fun updateUserStatus(
        @PathVariable id: Long, @Valid @RequestBody request: UpdateUserStatusRequest
    ): UserResponse {
        requireAdmin()
        return userService.setActive(id, request.active)
    }

    @DeleteMapping("/users/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteUser(@PathVariable id: Long) {
        requireAdmin()
        userService.deleteUser(id)
    }

    @GetMapping("/listings")
    fun searchListings(
        @RequestParam(required = false) status: String?,
        @PageableDefault(size = 20) pageable: Pageable
    ): Page<ListingResponse> {
        requireAdmin()
        val page = listingService.adminSearchListings(status, pageable)
        return page.map { ListingResponse.from(it) }
    }

    @GetMapping("/applications")
    fun searchApplications(
        @RequestParam(required = false) status: String?,
        @PageableDefault(size = 20) pageable: Pageable
    ): Page<ApplicationResponse> {
        requireAdmin()
        val page = applicationService.adminListApplications(status, pageable)
        return page.map { ApplicationResponse.from(it) }
    }

    private fun requireAdmin() {
        val currentUser = authService.requireCurrentUser()
        if (!currentUser.isAdmin()) {
            throw ForbiddenOperationException("Admin access required")
        }
    }
}
