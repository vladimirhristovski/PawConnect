package com.sorsix.pawconnect.repository

import com.sorsix.pawconnect.domain.BusinessPhoto
import org.springframework.data.jpa.repository.JpaRepository

interface BusinessPhotoRepository : JpaRepository<BusinessPhoto, Long>
