package com.sorsix.pawconnect.repository

import com.sorsix.pawconnect.model.Municipality
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface MunicipalityRepository : JpaRepository<Municipality, Long> {
    fun findByCode(code: String): Municipality?

    @Query(
        """
        SELECT m FROM Municipality m
        JOIN FETCH m.city
        WHERE (:cityCode IS NULL OR m.city.code = :cityCode)
    """
    )
    fun findWithCityByCityCode(@Param("cityCode") cityCode: String?): List<Municipality>
}