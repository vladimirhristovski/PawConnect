package com.sorsix.pawconnect.api

import com.sorsix.pawconnect.common.problemResponse
import com.sorsix.pawconnect.dto.request.UpdateUserStatusRequest
import com.sorsix.pawconnect.dto.response.ApplicationResponse
import com.sorsix.pawconnect.dto.response.ListingResponse
import com.sorsix.pawconnect.service.AdoptionApplicationService
import com.sorsix.pawconnect.service.ListingService
import com.sorsix.pawconnect.service.UserService
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin")
class AdminController(
    private val userService: UserService,
    private val listingService: ListingService,
    private val applicationService: AdoptionApplicationService
) {

    @GetMapping("/users")
    fun searchUsers(
        @RequestParam(required = false) active: Boolean?,
        @RequestParam(required = false) role: String?,
        pageable: Pageable
    ): ResponseEntity<*> {
        return ResponseEntity.ok(userService.searchUsers(active, role, pageable))
    }

    @PatchMapping("/users/{id}/status")
    fun updateUserStatus(
        @PathVariable id: Long, @Valid @RequestBody request: UpdateUserStatusRequest
    ): ResponseEntity<*> {
        return userService.setActive(id, request.active)?.let { ResponseEntity.ok(it) }
            ?: problemResponse(HttpStatus.NOT_FOUND, "User not found: $id")
    }

    @DeleteMapping("/users/{id}")
    fun deleteUser(@PathVariable id: Long): ResponseEntity<*> {
        return if (userService.deleteUser(id)) ResponseEntity.noContent().build<Unit>()
        else problemResponse(HttpStatus.NOT_FOUND, "User not found: $id")
    }

    @GetMapping("/listings")
    fun searchListings(
        @RequestParam(required = false) status: String?,
        pageable: Pageable
    ): ResponseEntity<*> {
        val page = listingService.adminSearchListings(status, pageable)
        return ResponseEntity.ok(page.map { ListingResponse.from(it) })
    }

    @GetMapping("/applications")
    fun searchApplications(
        @RequestParam(required = false) status: String?,
        pageable: Pageable
    ): ResponseEntity<*> {
        val page = applicationService.adminListApplications(status, pageable)
        return ResponseEntity.ok(page.map { ApplicationResponse.from(it) })
    }
}
