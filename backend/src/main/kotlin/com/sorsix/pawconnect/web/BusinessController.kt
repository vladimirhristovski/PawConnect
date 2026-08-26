package com.sorsix.pawconnect.web

import com.sorsix.pawconnect.dto.request.CreateBusinessRequest
import com.sorsix.pawconnect.dto.request.UpdateBusinessRequest
import com.sorsix.pawconnect.dto.response.BusinessResponse
import com.sorsix.pawconnect.service.AuthService
import com.sorsix.pawconnect.service.BusinessService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal

@RestController
@RequestMapping("/api/businesses")
class BusinessController(
    private val businessService: BusinessService, private val authService: AuthService
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createBusiness(@Valid @RequestBody request: CreateBusinessRequest): BusinessResponse {
        val currentUser = authService.requireCurrentUser()
        return businessService.createBusiness(request, currentUser)
    }

    @GetMapping
    fun searchBusinesses(
        @RequestParam(required = false) typeCode: String?,
        @RequestParam(required = false) municipalityCode: String?,
        @RequestParam(required = false) lat: BigDecimal?,
        @RequestParam(required = false) lng: BigDecimal?,
        @RequestParam(required = false) radiusKm: Double?,
        @PageableDefault(size = 20) pageable: Pageable
    ): Page<BusinessResponse> {
        if (lat != null || lng != null || radiusKm != null) {
            if (lat == null || lng == null || radiusKm == null) {
                throw IllegalArgumentException("lat, lng, and radiusKm must all be provided together")
            }
            return businessService.searchNearby(lat, lng, radiusKm, typeCode, pageable)
        }
        return businessService.searchBusinesses(typeCode, municipalityCode, pageable)
    }

    @GetMapping("/{id}")
    fun getBusiness(@PathVariable id: Long): BusinessResponse {
        return businessService.getBusinessOrThrow(id)
    }

    @PutMapping("/{id}")
    fun updateBusiness(
        @PathVariable id: Long, @Valid @RequestBody request: UpdateBusinessRequest
    ): BusinessResponse {
        val currentUser = authService.requireCurrentUser()
        return businessService.updateBusiness(id, request, currentUser)
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteBusiness(@PathVariable id: Long) {
        val currentUser = authService.requireCurrentUser()
        businessService.deleteBusiness(id, currentUser)
    }
}