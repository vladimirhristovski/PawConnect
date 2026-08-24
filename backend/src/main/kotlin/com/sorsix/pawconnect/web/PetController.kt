package com.sorsix.pawconnect.web

import com.sorsix.pawconnect.dto.request.PetPhotoRequest
import com.sorsix.pawconnect.dto.request.UpdatePetRequest
import com.sorsix.pawconnect.dto.response.PetPhotoResponse
import com.sorsix.pawconnect.dto.response.PetResponse
import com.sorsix.pawconnect.service.AuthService
import com.sorsix.pawconnect.service.PetService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/pets")
class PetController(
    private val petService: PetService, private val authService: AuthService
) {

    @GetMapping("/{id}")
    fun getPet(@PathVariable id: Long): PetResponse {
        val pet = petService.getPetOrThrow(id)
        return PetResponse.from(pet)
    }

    @PutMapping("/{id}")
    fun updatePet(@PathVariable id: Long, @Valid @RequestBody request: UpdatePetRequest): PetResponse {
        val currentUser = authService.requireCurrentUser()
        val pet = petService.updatePet(id, request, currentUser)
        return PetResponse.from(pet)
    }

    @PostMapping("/{id}/photos")
    @ResponseStatus(HttpStatus.CREATED)
    fun addPhoto(@PathVariable id: Long, @Valid @RequestBody request: PetPhotoRequest): PetPhotoResponse {
        val currentUser = authService.requireCurrentUser()
        val photo = petService.addPhoto(id, request, currentUser)
        return PetPhotoResponse.from(photo)
    }

    @PostMapping("/{id}/photos/upload", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun uploadPhoto(
        @PathVariable id: Long,
        @RequestPart("file") file: MultipartFile,
        @RequestParam(defaultValue = "false") isPrimary: Boolean,
        @RequestParam(defaultValue = "0") displayOrder: Int
    ): PetPhotoResponse {
        val currentUser = authService.requireCurrentUser()
        val photo = petService.uploadAndAddPhoto(id, file, isPrimary, displayOrder, currentUser)
        return PetPhotoResponse.from(photo)
    }

    @DeleteMapping("/{id}/photos/{photoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun removePhoto(@PathVariable id: Long, @PathVariable photoId: Long) {
        val currentUser = authService.requireCurrentUser()
        petService.removePhoto(id, photoId, currentUser)
    }
}