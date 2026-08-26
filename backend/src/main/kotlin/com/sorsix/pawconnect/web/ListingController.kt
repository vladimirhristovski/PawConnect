package com.sorsix.pawconnect.web

import com.sorsix.pawconnect.dto.request.CreateListingRequest
import com.sorsix.pawconnect.dto.request.UpdateListingRequest
import com.sorsix.pawconnect.dto.response.ListingResponse
import com.sorsix.pawconnect.dto.response.ListingSummaryResponse
import com.sorsix.pawconnect.exception.ResourceNotFoundException
import com.sorsix.pawconnect.model.enums.Gender
import com.sorsix.pawconnect.model.enums.Size
import com.sorsix.pawconnect.service.AuthService
import com.sorsix.pawconnect.service.ListingService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal

@RestController
@RequestMapping("/api/listings")
class ListingController(
    private val listingService: ListingService,
    private val authService: AuthService
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createListing(@Valid @RequestBody request: CreateListingRequest): ListingResponse {
        val currentUser = authService.requireCurrentUser()
        val listing = listingService.createListing(request, currentUser)
        return ListingResponse.from(listing)
    }

    @GetMapping
    fun searchListings(
        @RequestParam(required = false) speciesCode: String?,
        @RequestParam(required = false) municipalityCode: String?,
        @RequestParam(required = false) petSize: Size?,
        @RequestParam(required = false) gender: Gender?,
        @RequestParam(required = false) goodWithKids: Boolean?,
        @RequestParam(required = false) goodWithOtherPets: Boolean?,
        @RequestParam(required = false) minFee: BigDecimal?,
        @RequestParam(required = false) maxFee: BigDecimal?,
        @RequestParam(required = false) lat: BigDecimal?,
        @RequestParam(required = false) lng: BigDecimal?,
        @RequestParam(required = false) radiusKm: Double?,
        @PageableDefault(size = 20) pageable: Pageable
    ): Page<ListingSummaryResponse> {
        if (lat != null || lng != null || radiusKm != null) {
            if (lat == null || lng == null || radiusKm == null) {
                throw IllegalArgumentException("lat, lng, and radiusKm must all be provided together")
            }
            val nearbyPage = listingService.searchNearby(lat, lng, radiusKm, speciesCode, pageable)
            return nearbyPage.map { ListingSummaryResponse.from(it) }
        }
        val page = listingService.searchListings(
            speciesCode, municipalityCode, petSize, gender,
            goodWithKids, goodWithOtherPets, minFee, maxFee, pageable
        )
        return page.map { ListingSummaryResponse.from(it) }
    }

    @GetMapping("/mine")
    fun listMyListings(@PageableDefault(size = 20) pageable: Pageable): Page<ListingResponse> {
        val currentUser = authService.requireCurrentUser()
        val page = listingService.listMyListings(currentUser, pageable)
        return page.map { ListingResponse.from(it) }
    }

    @GetMapping("/{id}")
    fun getListing(@PathVariable id: Long): ListingResponse {
        val currentUser = authService.getCurrentUser()
        val listing = listingService.getVisibleListing(id, currentUser)
            ?: throw ResourceNotFoundException("Listing not found")
        return ListingResponse.from(listing)
    }

    @PutMapping("/{id}")
    fun updateListing(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateListingRequest
    ): ListingResponse {
        val currentUser = authService.requireCurrentUser()
        val listing = listingService.updateListing(id, request, currentUser)
        return ListingResponse.from(listing)
    }

    @PostMapping("/{id}/publish")
    fun publishListing(@PathVariable id: Long): ListingResponse {
        val currentUser = authService.requireCurrentUser()
        val listing = listingService.publishListing(id, currentUser)
        return ListingResponse.from(listing)
    }

    @PostMapping("/{id}/cancel")
    fun cancelListing(@PathVariable id: Long): ListingResponse {
        val currentUser = authService.requireCurrentUser()
        val listing = listingService.cancelListing(id, currentUser)
        return ListingResponse.from(listing)
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteListing(@PathVariable id: Long) {
        val currentUser = authService.requireCurrentUser()
        listingService.deleteListing(id, currentUser)
    }
}