package com.sorsix.pawconnect.repository

import com.sorsix.pawconnect.model.Listing
import org.springframework.data.jpa.repository.JpaRepository

interface ListingRepository : JpaRepository<Listing, Long> {
    fun existsByPet_IdAndPostedBy_Id(petId: Long, userId: Long): Boolean
}