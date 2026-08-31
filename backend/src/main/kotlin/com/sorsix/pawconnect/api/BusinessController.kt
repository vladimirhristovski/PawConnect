package com.sorsix.pawconnect.api

import com.sorsix.pawconnect.dto.request.BusinessPhotoRequest
import com.sorsix.pawconnect.dto.request.CreateBusinessRequest
import com.sorsix.pawconnect.dto.request.UpdateBusinessRequest
import com.sorsix.pawconnect.dto.response.BusinessPhotoResponse
import com.sorsix.pawconnect.dto.response.BusinessResponse
import com.sorsix.pawconnect.service.AuthService
import com.sorsix.pawconnect.service.BusinessService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
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
        resolveNearbySearch(lat, lng, radiusKm)?.let {
            return businessService.searchNearby(it.lat, it.lng, it.radiusKm, typeCode, pageable)
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

    @PostMapping("/{id}/photos")
    @ResponseStatus(HttpStatus.CREATED)
    fun addPhoto(@PathVariable id: Long, @Valid @RequestBody request: BusinessPhotoRequest): BusinessPhotoResponse {
        val currentUser = authService.requireCurrentUser()
        return businessService.addPhoto(id, request, currentUser)
    }

    @PostMapping("/{id}/photos/upload", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun uploadPhoto(
        @PathVariable id: Long,
        @RequestPart("file") file: MultipartFile,
        @RequestParam(defaultValue = "false") isPrimary: Boolean,
        @RequestParam(defaultValue = "0") displayOrder: Int
    ): BusinessPhotoResponse {
        val currentUser = authService.requireCurrentUser()
        return businessService.uploadAndAddPhoto(id, file, isPrimary, displayOrder, currentUser)
    }

    @DeleteMapping("/{id}/photos/{photoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun removePhoto(@PathVariable id: Long, @PathVariable photoId: Long) {
        val currentUser = authService.requireCurrentUser()
        businessService.removePhoto(id, photoId, currentUser)
    }
}