package com.sorsix.pawconnect.api

import com.sorsix.pawconnect.common.problemResponse
import com.sorsix.pawconnect.domain.result.AddBusinessPhotoResult
import com.sorsix.pawconnect.domain.result.CreateBusinessResult
import com.sorsix.pawconnect.domain.result.DeleteBusinessResult
import com.sorsix.pawconnect.domain.result.RemoveBusinessPhotoResult
import com.sorsix.pawconnect.domain.result.UpdateBusinessResult
import com.sorsix.pawconnect.dto.request.BusinessPhotoRequest
import com.sorsix.pawconnect.dto.request.CreateBusinessRequest
import com.sorsix.pawconnect.dto.request.UpdateBusinessRequest
import com.sorsix.pawconnect.dto.response.BusinessResponse
import com.sorsix.pawconnect.service.AuthService
import com.sorsix.pawconnect.service.BusinessService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal

@RestController
@RequestMapping("/api/businesses")
class BusinessController(
    private val businessService: BusinessService, private val authService: AuthService
) {

    @PostMapping
    fun createBusiness(@Valid @RequestBody request: CreateBusinessRequest): ResponseEntity<*> {
        val currentUser = authService.requireCurrentUser()
        return when (val result = businessService.createBusiness(request, currentUser)) {
            is CreateBusinessResult.Success -> ResponseEntity.status(HttpStatus.CREATED).body(result.business)
            is CreateBusinessResult.NotFound -> problemResponse(HttpStatus.NOT_FOUND, result.message)
        }
    }

    @GetMapping
    fun searchBusinesses(
        @RequestParam(required = false) typeCode: String?,
        @RequestParam(required = false) municipalityCode: String?,
        @RequestParam(required = false) lat: BigDecimal?,
        @RequestParam(required = false) lng: BigDecimal?,
        @RequestParam(required = false) radiusKm: Double?,
        pageable: Pageable
    ): Page<BusinessResponse> {
        resolveNearbySearch(lat, lng, radiusKm)?.let {
            return businessService.searchNearby(it.lat, it.lng, it.radiusKm, typeCode, municipalityCode, pageable)
        }
        return businessService.searchBusinesses(typeCode, municipalityCode, pageable)
    }

    @GetMapping("/{id}")
    fun getBusiness(@PathVariable id: Long): ResponseEntity<*> =
        businessService.findBusiness(id)?.let { ResponseEntity.ok(it) }
            ?: problemResponse(HttpStatus.NOT_FOUND, "Business not found: $id")

    @PutMapping("/{id}")
    fun updateBusiness(
        @PathVariable id: Long, @Valid @RequestBody request: UpdateBusinessRequest
    ): ResponseEntity<*> {
        val currentUser = authService.requireCurrentUser()
        return when (val result = businessService.updateBusiness(id, request, currentUser)) {
            is UpdateBusinessResult.Success -> ResponseEntity.ok(result.business)
            is UpdateBusinessResult.NotFound -> problemResponse(HttpStatus.NOT_FOUND, result.message)
            is UpdateBusinessResult.Forbidden -> problemResponse(HttpStatus.FORBIDDEN, result.message)
        }
    }

    @DeleteMapping("/{id}")
    fun deleteBusiness(@PathVariable id: Long): ResponseEntity<*> {
        val currentUser = authService.requireCurrentUser()
        return when (val result = businessService.deleteBusiness(id, currentUser)) {
            is DeleteBusinessResult.Success -> ResponseEntity.noContent().build<Unit>()
            is DeleteBusinessResult.NotFound -> problemResponse(HttpStatus.NOT_FOUND, result.message)
            is DeleteBusinessResult.Forbidden -> problemResponse(HttpStatus.FORBIDDEN, result.message)
        }
    }

    @PostMapping("/{id}/photos")
    fun addPhoto(@PathVariable id: Long, @Valid @RequestBody request: BusinessPhotoRequest): ResponseEntity<*> {
        val currentUser = authService.requireCurrentUser()
        return when (val result = businessService.addPhoto(id, request, currentUser)) {
            is AddBusinessPhotoResult.Success -> ResponseEntity.status(HttpStatus.CREATED).body(result.photo)
            is AddBusinessPhotoResult.NotFound -> problemResponse(HttpStatus.NOT_FOUND, result.message)
            is AddBusinessPhotoResult.Forbidden -> problemResponse(HttpStatus.FORBIDDEN, result.message)
        }
    }

    @PostMapping("/{id}/photos/upload", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadPhoto(
        @PathVariable id: Long,
        @RequestPart("file") file: MultipartFile,
        @RequestParam(defaultValue = "false") isPrimary: Boolean,
        @RequestParam(defaultValue = "0") displayOrder: Int
    ): ResponseEntity<*> {
        val currentUser = authService.requireCurrentUser()
        return when (val result = businessService.uploadAndAddPhoto(id, file, isPrimary, displayOrder, currentUser)) {
            is AddBusinessPhotoResult.Success -> ResponseEntity.status(HttpStatus.CREATED).body(result.photo)
            is AddBusinessPhotoResult.NotFound -> problemResponse(HttpStatus.NOT_FOUND, result.message)
            is AddBusinessPhotoResult.Forbidden -> problemResponse(HttpStatus.FORBIDDEN, result.message)
        }
    }

    @DeleteMapping("/{id}/photos/{photoId}")
    fun removePhoto(@PathVariable id: Long, @PathVariable photoId: Long): ResponseEntity<*> {
        val currentUser = authService.requireCurrentUser()
        return when (val result = businessService.removePhoto(id, photoId, currentUser)) {
            is RemoveBusinessPhotoResult.Success -> ResponseEntity.noContent().build<Unit>()
            is RemoveBusinessPhotoResult.NotFound -> problemResponse(HttpStatus.NOT_FOUND, result.message)
            is RemoveBusinessPhotoResult.Forbidden -> problemResponse(HttpStatus.FORBIDDEN, result.message)
        }
    }
}
