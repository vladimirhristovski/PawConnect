package com.sorsix.pawconnect.dto.request

data class PetPhotoRequest(
    val url: String,
    val isPrimary: Boolean = false,
    val displayOrder: Int = 0
)