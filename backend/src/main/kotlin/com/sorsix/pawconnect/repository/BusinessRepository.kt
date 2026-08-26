package com.sorsix.pawconnect.repository

import com.sorsix.pawconnect.model.Business
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface BusinessRepository : JpaRepository<Business, Long>, JpaSpecificationExecutor<Business> {
    @Query("SELECT b FROM Business b JOIN FETCH b.type JOIN FETCH b.municipality LEFT JOIN FETCH b.owner WHERE b.id = :id")
    fun findByIdWithAssociations(@Param("id") id: Long): Business?

    fun findByAddressGeocodedFalseAndDeletedAtIsNull(): List<Business>

    @Query(
        value = """
            SELECT b.*
            FROM businesses b
            JOIN business_types bt ON bt.id = b.type_id
            WHERE b.deleted_at IS NULL
              AND b.latitude IS NOT NULL
              AND b.longitude IS NOT NULL
              AND (:typeCode IS NULL OR bt.code = :typeCode)
              AND 6371 * acos(
                    LEAST(1.0, GREATEST(-1.0,
                      cos(radians(CAST(:lat AS double precision))) * cos(radians(CAST(b.latitude AS double precision))) *
                      cos(radians(CAST(b.longitude AS double precision)) - radians(CAST(:lng AS double precision)))
                      + sin(radians(CAST(:lat AS double precision))) * sin(radians(CAST(b.latitude AS double precision)))
                    ))
                  ) <= :radiusKm
            ORDER BY 6371 * acos(
                    LEAST(1.0, GREATEST(-1.0,
                      cos(radians(CAST(:lat AS double precision))) * cos(radians(CAST(b.latitude AS double precision))) *
                      cos(radians(CAST(b.longitude AS double precision)) - radians(CAST(:lng AS double precision)))
                      + sin(radians(CAST(:lat AS double precision))) * sin(radians(CAST(b.latitude AS double precision)))
                    ))
                  ) ASC
        """,
        countQuery = """
            SELECT count(*)
            FROM businesses b
            JOIN business_types bt ON bt.id = b.type_id
            WHERE b.deleted_at IS NULL
              AND b.latitude IS NOT NULL
              AND b.longitude IS NOT NULL
              AND (:typeCode IS NULL OR bt.code = :typeCode)
              AND 6371 * acos(
                    LEAST(1.0, GREATEST(-1.0,
                      cos(radians(CAST(:lat AS double precision))) * cos(radians(CAST(b.latitude AS double precision))) *
                      cos(radians(CAST(b.longitude AS double precision)) - radians(CAST(:lng AS double precision)))
                      + sin(radians(CAST(:lat AS double precision))) * sin(radians(CAST(b.latitude AS double precision)))
                    ))
                  ) <= :radiusKm
        """,
        nativeQuery = true
    )
    fun findNearby(
        @Param("lat") lat: Double,
        @Param("lng") lng: Double,
        @Param("radiusKm") radiusKm: Double,
        @Param("typeCode") typeCode: String?,
        pageable: Pageable
    ): Page<Business>
}