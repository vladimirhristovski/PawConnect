package com.sorsix.pawconnect.repository

import com.sorsix.pawconnect.model.Pet
import org.springframework.data.jpa.repository.JpaRepository

interface PetRepository : JpaRepository<Pet, Long>