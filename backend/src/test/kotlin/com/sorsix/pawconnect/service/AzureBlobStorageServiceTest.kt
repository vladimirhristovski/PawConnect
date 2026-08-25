package com.sorsix.pawconnect.service

import com.azure.storage.blob.BlobClient
import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.blob.models.BlobHttpHeaders
import com.sorsix.pawconnect.exception.BlobStorageException
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AzureBlobStorageServiceTest {

    private val containerClient = mockk<BlobContainerClient>()
    private val service = AzureBlobStorageService(containerClient)

    private fun mockFile(
        contentType: String = "image/jpeg",
        size: Long = 1024,
        empty: Boolean = false
    ): MultipartFile {
        val file = mockk<MultipartFile>()
        every { file.contentType } returns contentType
        every { file.size } returns size
        every { file.isEmpty } returns empty
        every { file.inputStream } returns ByteArrayInputStream(ByteArray(size.toInt()))
        return file
    }

    @Test
    fun `upload rejects unsupported content type`() {
        val file = mockFile(contentType = "text/plain")
        assertFailsWith<IllegalArgumentException> {
            service.upload(file, "pets/1")
        }
    }

    @Test
    fun `upload rejects file over the size limit`() {
        val file = mockFile(size = 6L * 1024 * 1024)
        assertFailsWith<IllegalArgumentException> {
            service.upload(file, "pets/1")
        }
    }

    @Test
    fun `upload rejects empty file`() {
        val file = mockFile(empty = true)
        assertFailsWith<IllegalArgumentException> {
            service.upload(file, "pets/1")
        }
    }

    @Test
    fun `upload happy path streams to blob client and returns its url`() {
        val file = mockFile(contentType = "image/png")
        val blobClient = mockk<BlobClient>(relaxed = true)
        every { containerClient.getBlobClient(any()) } returns blobClient
        every { blobClient.blobUrl } returns "http://127.0.0.1:10000/devstoreaccount1/pet-photos/pets/1/generated.png"

        val result = service.upload(file, "pets/1")

        assertEquals("http://127.0.0.1:10000/devstoreaccount1/pet-photos/pets/1/generated.png", result)
        verify { blobClient.upload(any(), 1024, true) }
        verify { blobClient.setHttpHeaders(any<BlobHttpHeaders>()) }
    }

    @Test
    fun `upload wraps SDK failures in BlobStorageException`() {
        val file = mockFile()
        every { containerClient.getBlobClient(any()) } throws RuntimeException("network down")

        assertFailsWith<BlobStorageException> {
            service.upload(file, "pets/1")
        }
    }

    @Test
    fun `delete resolves the real blob name from a URL-encoded URL, not a double-encoded one`() {
        val blobClient = mockk<BlobClient>(relaxed = true)
        val nameSlot = slot<String>()

        every { containerClient.blobContainerName } returns "pet-photos"
        val storedUrl = "http://127.0.0.1:10000/devstoreaccount1/pet-photos/pets%2F5%2Fabc-123.jpg"

        every { containerClient.getBlobClient(capture(nameSlot)) } returns blobClient
        every { blobClient.exists() } returns true

        service.delete(storedUrl)

        assertEquals("pets/5/abc-123.jpg", nameSlot.captured)
        verify(exactly = 1) { blobClient.delete() }
    }

    @Test
    fun `delete is a no-op when the blob doesn't exist`() {
        val blobClient = mockk<BlobClient>(relaxed = true)
        every { containerClient.blobContainerName } returns "pet-photos"
        every { containerClient.getBlobClient(any()) } returns blobClient
        every { blobClient.exists() } returns false

        service.delete("http://127.0.0.1:10000/devstoreaccount1/pet-photos/pets%2F5%2Fabc-123.jpg")

        verify(exactly = 0) { blobClient.delete() }
    }

    @Test
    fun `delete is a no-op for a URL from a different container`() {
        every { containerClient.blobContainerName } returns "pet-photos"
        service.delete("https://example.com/some-other-photo.jpg")
        verify(exactly = 0) { containerClient.getBlobClient(any()) }
    }

    @Test
    fun `delete wraps SDK failures in BlobStorageException`() {
        every { containerClient.blobContainerName } returns "pet-photos"
        every { containerClient.getBlobClient(any()) } throws RuntimeException("network down")

        assertFailsWith<BlobStorageException> {
            service.delete("http://127.0.0.1:10000/devstoreaccount1/pet-photos/pets%2F5%2Fabc-123.jpg")
        }
    }
}