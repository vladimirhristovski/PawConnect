package com.sorsix.pawconnect.repository

import com.sorsix.pawconnect.domain.Country
import org.springframework.data.jpa.repository.JpaRepository

interface CountryRepository : JpaRepository<Country, Long>