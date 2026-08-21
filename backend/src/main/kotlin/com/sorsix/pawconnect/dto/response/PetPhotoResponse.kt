package com.sorsix.pawconnect.dto.response

import com.sorsix.pawconnect.model.PetPhoto

data class PetPhotoResponse(
    val id: Long, val url: String, val isPrimary: Boolean, val displayOrder: Int
) {
    companion object {
        fun from(photo: PetPhoto) = PetPhotoResponse(
            id = photo.id!!, url = photo.url, isPrimary = photo.isPrimary, displayOrder = photo.displayOrder
        )
    }
}