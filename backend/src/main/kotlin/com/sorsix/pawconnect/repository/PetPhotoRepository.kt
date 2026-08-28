package com.sorsix.pawconnect.repository

import com.sorsix.pawconnect.domain.PetPhoto
import org.springframework.data.jpa.repository.JpaRepository

interface PetPhotoRepository : JpaRepository<PetPhoto, Long> {
    fun findByPet_IdOrderByDisplayOrderAsc(petId: Long): List<PetPhoto>
}