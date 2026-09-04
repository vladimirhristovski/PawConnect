package com.sorsix.pawconnect.common

import com.sorsix.pawconnect.domain.Photo

fun normalizePrimaryPhoto(photos: Collection<Photo>) {
    val primaries = photos.filter { it.isPrimary }
    if (primaries.size > 1) {
        primaries.drop(1).forEach { it.isPrimary = false }
    } else if (primaries.isEmpty() && photos.isNotEmpty()) {
        photos.minByOrNull { it.displayOrder }?.isPrimary = true
    }
}
