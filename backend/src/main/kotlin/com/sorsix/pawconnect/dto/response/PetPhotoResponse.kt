package com.sorsix.pawconnect.dto.response

import com.sorsix.pawconnect.common.requireId
import com.sorsix.pawconnect.domain.PetPhoto

data class PetPhotoResponse(
    val id: Long,
    val url: String,
    val isPrimary: Boolean,
    val displayOrder: Int,
) {
    companion object {
        fun from(photo: PetPhoto) =
            PetPhotoResponse(
                id = photo.requireId(),
                url = photo.url,
                isPrimary = photo.isPrimary,
                displayOrder = photo.displayOrder,
            )
    }
}
