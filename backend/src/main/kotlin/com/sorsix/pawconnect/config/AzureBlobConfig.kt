package com.sorsix.pawconnect.config

import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.blob.BlobServiceClient
import com.azure.storage.blob.BlobServiceClientBuilder
import com.azure.storage.blob.models.PublicAccessType
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("!test")
class AzureBlobConfig(
    @Value("\${azure.storage.connection-string}") private val connectionString: String,
    @Value("\${azure.storage.container-name}") private val containerName: String
) {

    @Bean
    fun blobServiceClient(): BlobServiceClient =
        BlobServiceClientBuilder()
            .connectionString(connectionString)
            .buildClient()

    @Bean
    fun blobContainerClient(blobServiceClient: BlobServiceClient): BlobContainerClient {
        val client = blobServiceClient.getBlobContainerClient(containerName)
        if (!client.exists()) {
            client.create()
            client.setAccessPolicy(PublicAccessType.BLOB, null)
        }
        return client
    }
}