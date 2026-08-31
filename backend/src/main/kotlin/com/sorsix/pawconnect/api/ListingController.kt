package com.sorsix.pawconnect.api

import com.sorsix.pawconnect.common.problemResponse
import com.sorsix.pawconnect.domain.result.CancelListingResult
import com.sorsix.pawconnect.domain.result.CreateListingResult
import com.sorsix.pawconnect.domain.result.DeleteListingResult
import com.sorsix.pawconnect.domain.result.PublishListingResult
import com.sorsix.pawconnect.domain.result.UpdateListingResult
import com.sorsix.pawconnect.dto.request.CreateListingRequest
import com.sorsix.pawconnect.dto.request.UpdateListingRequest
import com.sorsix.pawconnect.dto.response.ListingResponse
import com.sorsix.pawconnect.dto.response.ListingSummaryResponse
import com.sorsix.pawconnect.domain.Gender
import com.sorsix.pawconnect.domain.Size
import com.sorsix.pawconnect.service.AuthService
import com.sorsix.pawconnect.service.ListingService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal

@RestController
@RequestMapping("/api/listings")
class ListingController(
    private val listingService: ListingService,
    private val authService: AuthService
) {

    @PostMapping
    fun createListing(@Valid @RequestBody request: CreateListingRequest): ResponseEntity<*> {
        val currentUser = authService.requireCurrentUser()
        return when (val result = listingService.createListing(request, currentUser)) {
            is CreateListingResult.Success -> ResponseEntity.status(HttpStatus.CREATED).body(ListingResponse.from(result.listing))
            is CreateListingResult.NotFound -> problemResponse(HttpStatus.NOT_FOUND, result.message)
            is CreateListingResult.Conflict -> problemResponse(HttpStatus.CONFLICT, result.message)
            is CreateListingResult.Forbidden -> problemResponse(HttpStatus.FORBIDDEN, result.message)
        }
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
        resolveNearbySearch(lat, lng, radiusKm)?.let {
            return listingService.searchNearby(it.lat, it.lng, it.radiusKm, speciesCode, pageable)
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
    fun getListing(@PathVariable id: Long): ResponseEntity<*> {
        val currentUser = authService.getCurrentUser()
        return listingService.getVisibleListing(id, currentUser)?.let { ResponseEntity.ok(ListingResponse.from(it)) }
            ?: problemResponse(HttpStatus.NOT_FOUND, "Listing not found")
    }

    @PutMapping("/{id}")
    fun updateListing(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateListingRequest
    ): ResponseEntity<*> {
        val currentUser = authService.requireCurrentUser()
        return when (val result = listingService.updateListing(id, request, currentUser)) {
            is UpdateListingResult.Success -> ResponseEntity.ok(ListingResponse.from(result.listing))
            is UpdateListingResult.NotFound -> problemResponse(HttpStatus.NOT_FOUND, result.message)
            is UpdateListingResult.Conflict -> problemResponse(HttpStatus.CONFLICT, result.message)
            is UpdateListingResult.Forbidden -> problemResponse(HttpStatus.FORBIDDEN, result.message)
        }
    }

    @PostMapping("/{id}/publish")
    fun publishListing(@PathVariable id: Long): ResponseEntity<*> {
        val currentUser = authService.requireCurrentUser()
        return when (val result = listingService.publishListing(id, currentUser)) {
            is PublishListingResult.Success -> ResponseEntity.ok(ListingResponse.from(result.listing))
            is PublishListingResult.NotFound -> problemResponse(HttpStatus.NOT_FOUND, result.message)
            is PublishListingResult.Conflict -> problemResponse(HttpStatus.CONFLICT, result.message)
            is PublishListingResult.Forbidden -> problemResponse(HttpStatus.FORBIDDEN, result.message)
        }
    }

    @PostMapping("/{id}/cancel")
    fun cancelListing(@PathVariable id: Long): ResponseEntity<*> {
        val currentUser = authService.requireCurrentUser()
        return when (val result = listingService.cancelListing(id, currentUser)) {
            is CancelListingResult.Success -> ResponseEntity.ok(ListingResponse.from(result.listing))
            is CancelListingResult.NotFound -> problemResponse(HttpStatus.NOT_FOUND, result.message)
            is CancelListingResult.Conflict -> problemResponse(HttpStatus.CONFLICT, result.message)
            is CancelListingResult.Forbidden -> problemResponse(HttpStatus.FORBIDDEN, result.message)
        }
    }

    @DeleteMapping("/{id}")
    fun deleteListing(@PathVariable id: Long): ResponseEntity<*> {
        val currentUser = authService.requireCurrentUser()
        return when (val result = listingService.deleteListing(id, currentUser)) {
            is DeleteListingResult.Success -> ResponseEntity.noContent().build<Unit>()
            is DeleteListingResult.NotFound -> problemResponse(HttpStatus.NOT_FOUND, result.message)
            is DeleteListingResult.Forbidden -> problemResponse(HttpStatus.FORBIDDEN, result.message)
        }
    }
}
