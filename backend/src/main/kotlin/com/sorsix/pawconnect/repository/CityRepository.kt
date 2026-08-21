package com.sorsix.pawconnect.repository

import com.sorsix.pawconnect.model.City
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CityRepository : JpaRepository<City, Long> {
    fun findByCode(code: String): City?

    @Query(
        """
        SELECT c FROM City c
        JOIN FETCH c.country
        WHERE (:countryCode IS NULL OR c.country.code = :countryCode)
    """
    )
    fun findWithCountryByCountryCode(@Param("countryCode") countryCode: String?): List<City>
}