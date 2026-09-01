package com.sorsix.pawconnect.service

import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.blob.BlobUrlParts
import com.azure.storage.blob.models.BlobHttpHeaders
import com.sorsix.pawconnect.exception.BlobStorageException
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream
import java.util.UUID

@Service
@Profile("!test")
class AzureBlobStorageService(
    private val containerClient: BlobContainerClient
) : BlobStorageService {

    private val allowedContentTypes = setOf("image/jpeg", "image/png", "image/webp")
    private val maxSizeBytes = 5L * 1024 * 1024

    override fun upload(file: MultipartFile, folder: String): String {
        if (file.isEmpty) throw IllegalArgumentException("File must not be empty")
        if (file.contentType !in allowedContentTypes) {
            throw IllegalArgumentException("Unsupported file type: ${file.contentType}. Allowed: jpg, png, webp")
        }
        if (file.size > maxSizeBytes) {
            throw IllegalArgumentException("File too large (max 5MB)")
        }

        val bytes = file.bytes
        if (detectImageContentType(bytes) != file.contentType) {
            throw IllegalArgumentException("File content does not match declared type: ${file.contentType}")
        }

        val extension = when (file.contentType) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "bin"
        }
        val blobName = "$folder/${UUID.randomUUID()}.$extension"

        return try {
            val blobClient = containerClient.getBlobClient(blobName)
            ByteArrayInputStream(bytes).use { stream ->
                blobClient.upload(stream, bytes.size.toLong(), true)
            }
            blobClient.setHttpHeaders(BlobHttpHeaders().setContentType(file.contentType))
            blobClient.blobUrl
        } catch (ex: Exception) {
            throw BlobStorageException("Failed to upload file to blob storage", ex)
        }
    }

    override fun delete(blobUrl: String) {
        val blobName = extractBlobName(blobUrl) ?: return
        try {
            val blobClient = containerClient.getBlobClient(blobName)
            if (blobClient.exists()) blobClient.delete()
        } catch (ex: Exception) {
            throw BlobStorageException("Failed to delete blob: $blobUrl", ex)
        }
    }

    private fun extractBlobName(blobUrl: String): String? {
        return try {
            val parts = BlobUrlParts.parse(blobUrl)
            if (parts.blobContainerName != containerClient.blobContainerName) return null
            parts.blobName
        } catch (ex: Exception) {
            null
        }
    }

    private fun detectImageContentType(bytes: ByteArray): String? = when {
        bytes.size >= 3 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte() ->
            "image/jpeg"

        bytes.size >= 8 && bytes.copyOfRange(0, 8).contentEquals(PNG_SIGNATURE) ->
            "image/png"

        bytes.size >= 12 &&
            String(bytes, 0, 4, Charsets.US_ASCII) == "RIFF" &&
            String(bytes, 8, 4, Charsets.US_ASCII) == "WEBP" ->
            "image/webp"

        else -> null
    }

    companion object {
        private val PNG_SIGNATURE =
            byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
    }
}
