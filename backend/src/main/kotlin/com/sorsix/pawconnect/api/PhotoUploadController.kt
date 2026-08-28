package com.sorsix.pawconnect.api

import com.sorsix.pawconnect.dto.response.TempUploadResponse
import com.sorsix.pawconnect.service.AuthService
import com.sorsix.pawconnect.service.BlobStorageService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@RestController
@RequestMapping("/api/photos")
class PhotoUploadController(
    private val blobStorageService: BlobStorageService,
    private val authService: AuthService
) {

    @PostMapping("/upload", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun uploadTempPhoto(@RequestPart("file") file: MultipartFile): TempUploadResponse {
        authService.requireCurrentUser()
        val url = blobStorageService.upload(file, "temp/${UUID.randomUUID()}")
        return TempUploadResponse(url)
    }
}
