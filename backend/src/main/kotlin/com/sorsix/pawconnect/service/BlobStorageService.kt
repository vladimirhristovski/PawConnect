package com.sorsix.pawconnect.service

import org.springframework.web.multipart.MultipartFile

interface BlobStorageService {
    fun upload(file: MultipartFile, folder: String): String
    fun delete(blobUrl: String)
}