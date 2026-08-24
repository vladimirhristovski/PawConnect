package com.sorsix.pawconnect.service

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList


@Service
@Profile("test")
class TestBlobStorageService : BlobStorageService {

    private val allowedContentTypes = setOf("image/jpeg", "image/png", "image/webp")
    private val maxSizeBytes = 5L * 1024 * 1024

    val uploadedUrls = CopyOnWriteArrayList<String>()
    val deletedUrls = CopyOnWriteArrayList<String>()

    fun reset() {
        uploadedUrls.clear()
        deletedUrls.clear()
    }

    override fun upload(file: MultipartFile, folder: String): String {
        if (file.isEmpty) throw IllegalArgumentException("File must not be empty")
        if (file.contentType !in allowedContentTypes) {
            throw IllegalArgumentException("Unsupported file type: ${file.contentType}. Allowed: jpg, png, webp")
        }
        if (file.size > maxSizeBytes) {
            throw IllegalArgumentException("File too large (max 5MB)")
        }

        val extension = when (file.contentType) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "bin"
        }
        val url = "http://fake-blob.test/pet-photos/$folder/${UUID.randomUUID()}.$extension"
        uploadedUrls.add(url)
        return url
    }

    override fun delete(blobUrl: String) {
        deletedUrls.add(blobUrl)
    }
}