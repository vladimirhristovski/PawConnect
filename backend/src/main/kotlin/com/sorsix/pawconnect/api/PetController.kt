package com.sorsix.pawconnect.api

import com.sorsix.pawconnect.common.problemResponse
import com.sorsix.pawconnect.domain.result.AddPetPhotoResult
import com.sorsix.pawconnect.domain.result.RemovePetPhotoResult
import com.sorsix.pawconnect.domain.result.UpdatePetResult
import com.sorsix.pawconnect.dto.request.PetPhotoRequest
import com.sorsix.pawconnect.dto.request.UpdatePetRequest
import com.sorsix.pawconnect.dto.response.PetPhotoResponse
import com.sorsix.pawconnect.dto.response.PetResponse
import com.sorsix.pawconnect.service.AuthService
import com.sorsix.pawconnect.service.PetService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/pets")
class PetController(
    private val petService: PetService,
    private val authService: AuthService,
) {
    @GetMapping("/{id}")
    fun getPet(
        @PathVariable id: Long,
    ): ResponseEntity<*> {
        val currentUser = authService.getCurrentUser()
        return petService.getVisiblePet(id, currentUser)?.let { ResponseEntity.ok(PetResponse.from(it)) }
            ?: problemResponse(HttpStatus.NOT_FOUND, "Pet not found: $id")
    }

    @PutMapping("/{id}")
    fun updatePet(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdatePetRequest,
    ): ResponseEntity<*> {
        val currentUser = authService.requireCurrentUser()
        return when (val result = petService.updatePet(id, request, currentUser)) {
            is UpdatePetResult.Success -> ResponseEntity.ok(PetResponse.from(result.pet))
            is UpdatePetResult.NotFound -> problemResponse(HttpStatus.NOT_FOUND, result.message)
            is UpdatePetResult.Forbidden -> problemResponse(HttpStatus.FORBIDDEN, result.message)
        }
    }

    @PostMapping("/{id}/photos")
    fun addPhoto(
        @PathVariable id: Long,
        @Valid @RequestBody request: PetPhotoRequest,
    ): ResponseEntity<*> {
        val currentUser = authService.requireCurrentUser()
        return when (val result = petService.addPhoto(id, request, currentUser)) {
            is AddPetPhotoResult.Success -> ResponseEntity.status(HttpStatus.CREATED).body(PetPhotoResponse.from(result.photo))
            is AddPetPhotoResult.NotFound -> problemResponse(HttpStatus.NOT_FOUND, result.message)
            is AddPetPhotoResult.Forbidden -> problemResponse(HttpStatus.FORBIDDEN, result.message)
        }
    }

    @PostMapping("/{id}/photos/upload", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadPhoto(
        @PathVariable id: Long,
        @RequestPart("file") file: MultipartFile,
        @RequestParam(defaultValue = "false") isPrimary: Boolean,
        @RequestParam(defaultValue = "0") displayOrder: Int,
    ): ResponseEntity<*> {
        val currentUser = authService.requireCurrentUser()
        return when (val result = petService.uploadAndAddPhoto(id, file, isPrimary, displayOrder, currentUser)) {
            is AddPetPhotoResult.Success -> ResponseEntity.status(HttpStatus.CREATED).body(PetPhotoResponse.from(result.photo))
            is AddPetPhotoResult.NotFound -> problemResponse(HttpStatus.NOT_FOUND, result.message)
            is AddPetPhotoResult.Forbidden -> problemResponse(HttpStatus.FORBIDDEN, result.message)
        }
    }

    @DeleteMapping("/{id}/photos/{photoId}")
    fun removePhoto(
        @PathVariable id: Long,
        @PathVariable photoId: Long,
    ): ResponseEntity<*> {
        val currentUser = authService.requireCurrentUser()
        return when (val result = petService.removePhoto(id, photoId, currentUser)) {
            is RemovePetPhotoResult.Success -> ResponseEntity.noContent().build<Unit>()
            is RemovePetPhotoResult.NotFound -> problemResponse(HttpStatus.NOT_FOUND, result.message)
            is RemovePetPhotoResult.Forbidden -> problemResponse(HttpStatus.FORBIDDEN, result.message)
        }
    }
}
