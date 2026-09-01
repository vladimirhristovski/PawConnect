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
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AzureBlobStorageServiceTest {

    private val containerClient = mockk<BlobContainerClient>()
    private val service = AzureBlobStorageService(containerClient)

    private fun validImageBytes(contentType: String, size: Int): ByteArray {
        val header = when (contentType) {
            "image/jpeg" -> byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
            "image/png" -> byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
            "image/webp" -> byteArrayOf(
                'R'.code.toByte(), 'I'.code.toByte(), 'F'.code.toByte(), 'F'.code.toByte(),
                0, 0, 0, 0,
                'W'.code.toByte(), 'E'.code.toByte(), 'B'.code.toByte(), 'P'.code.toByte()
            )
            else -> ByteArray(0)
        }
        val result = ByteArray(maxOf(size, header.size))
        header.copyInto(result)
        return result
    }

    private fun mockFile(
        contentType: String = "image/jpeg",
        size: Long = 1024,
        empty: Boolean = false,
        bytes: ByteArray = validImageBytes(contentType, size.toInt())
    ): MultipartFile {
        val file = mockk<MultipartFile>()
        every { file.contentType } returns contentType
        every { file.size } returns size
        every { file.isEmpty } returns empty
        every { file.bytes } returns bytes
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
    fun `upload rejects when the file content does not match the declared content type`() {
        val file = mockFile(contentType = "image/jpeg", bytes = "this is not an image".toByteArray())
        assertFailsWith<IllegalArgumentException> {
            service.upload(file, "pets/1")
        }
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