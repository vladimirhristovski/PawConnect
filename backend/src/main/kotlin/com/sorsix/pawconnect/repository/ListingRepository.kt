package com.sorsix.pawconnect.repository

import com.sorsix.pawconnect.model.Listing
import com.sorsix.pawconnect.model.enums.Gender
import com.sorsix.pawconnect.model.enums.Size
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.query.Param
import java.math.BigDecimal
import java.time.Instant

interface ListingRepository : JpaRepository<Listing, Long>, JpaSpecificationExecutor<Listing> {

    fun existsByPet_IdAndPostedBy_Id(petId: Long, userId: Long): Boolean

    fun existsByPet_IdAndStatus_CodeInAndDeletedAtIsNull(petId: Long, codes: Collection<String>): Boolean

    fun findByPostedBy_IdAndDeletedAtIsNull(userId: Long, pageable: Pageable): Page<Listing>

    fun findByStatus_CodeAndExpiresAtBefore(statusCode: String, cutoff: Instant): List<Listing>

    @Query(
        """
        SELECT DISTINCT l FROM Listing l
        JOIN FETCH l.municipality m
        JOIN FETCH m.city c
        JOIN FETCH c.country
        JOIN FETCH l.status
        JOIN FETCH l.postedBy
        LEFT JOIN FETCH l.business b
        LEFT JOIN FETCH b.type
        LEFT JOIN FETCH b.municipality
        LEFT JOIN FETCH b.owner
        LEFT JOIN FETCH l.pet p
        LEFT JOIN FETCH p.species
        LEFT JOIN FETCH p.breeds
        LEFT JOIN FETCH p.photos
        WHERE l.id = :id
        """
    )
    fun findByIdWithAllAssociations(@Param("id") id: Long): Listing?

    @Query(
        """
    SELECT DISTINCT l FROM Listing l
    JOIN FETCH l.pet p
    JOIN FETCH p.species
    LEFT JOIN FETCH p.breeds
    LEFT JOIN FETCH p.photos
    JOIN FETCH l.municipality m
    JOIN FETCH m.city c
    JOIN FETCH c.country
    JOIN FETCH l.status
    JOIN FETCH l.postedBy
    LEFT JOIN FETCH l.business b
    LEFT JOIN FETCH b.type
    WHERE l.status.code = 'ACTIVE'
    AND (:speciesCode IS NULL OR p.species.code = :speciesCode)
    AND (:municipalityCode IS NULL OR m.code = :municipalityCode)
    AND (:petSize IS NULL OR p.size = :petSize)
    AND (:gender IS NULL OR p.gender = :gender)
    AND (:goodWithKids IS NULL OR p.goodWithKids = :goodWithKids)
    AND (:goodWithOtherPets IS NULL OR p.goodWithOtherPets = :goodWithOtherPets)
    AND (:minFee IS NULL OR l.adoptionFee >= :minFee)
    AND (:maxFee IS NULL OR l.adoptionFee <= :maxFee)
    """
    )
    fun searchListings(
        @Param("speciesCode") speciesCode: String?,
        @Param("municipalityCode") municipalityCode: String?,
        @Param("petSize") petSize: Size?,
        @Param("gender") gender: Gender?,
        @Param("goodWithKids") goodWithKids: Boolean?,
        @Param("goodWithOtherPets") goodWithOtherPets: Boolean?,
        @Param("minFee") minFee: BigDecimal?,
        @Param("maxFee") maxFee: BigDecimal?,
        pageable: Pageable
    ): Page<Listing>

    @Query(
        """
    SELECT DISTINCT l FROM Listing l
    JOIN FETCH l.pet p
    JOIN FETCH p.species
    LEFT JOIN FETCH p.breeds
    LEFT JOIN FETCH p.photos
    JOIN FETCH l.municipality m
    JOIN FETCH m.city c
    JOIN FETCH c.country
    JOIN FETCH l.status
    JOIN FETCH l.postedBy
    LEFT JOIN FETCH l.business b
    LEFT JOIN FETCH b.type
    LEFT JOIN FETCH b.municipality
    LEFT JOIN FETCH b.owner
    WHERE l.postedBy.id = :userId
    AND l.deletedAt IS NULL
    """
    )
    fun findMyListingsWithAssociations(@Param("userId") userId: Long, pageable: Pageable): Page<Listing>

    @Query(
        """
    SELECT DISTINCT l FROM Listing l
    JOIN FETCH l.pet p
    JOIN FETCH p.species
    LEFT JOIN FETCH p.breeds
    LEFT JOIN FETCH p.photos
    JOIN FETCH l.municipality m
    JOIN FETCH m.city c
    JOIN FETCH c.country
    JOIN FETCH l.status
    JOIN FETCH l.postedBy
    LEFT JOIN FETCH l.business b
    LEFT JOIN FETCH b.type
    LEFT JOIN FETCH b.municipality
    LEFT JOIN FETCH b.owner
    WHERE (:statusCode IS NULL OR l.status.code = :statusCode)
    AND l.deletedAt IS NULL
    """
    )
    fun findAllWithAssociations(@Param("statusCode") statusCode: String?, pageable: Pageable): Page<Listing>

    fun findByLatitudeIsNullAndDeletedAtIsNull(): List<Listing>

    @Query(
        value = """
            SELECT l.*
            FROM listings l
            JOIN listing_statuses ls ON ls.id = l.status_id
            JOIN pets p ON p.id = l.pet_id
            JOIN pet_species ps ON ps.id = p.species_id
            WHERE l.deleted_at IS NULL
              AND ls.code = 'ACTIVE'
              AND l.latitude IS NOT NULL
              AND l.longitude IS NOT NULL
              AND (:speciesCode IS NULL OR ps.code = :speciesCode)
              AND 6371 * acos(
                    LEAST(1.0, GREATEST(-1.0,
                      cos(radians(CAST(:lat AS double precision))) * cos(radians(CAST(l.latitude AS double precision))) *
                      cos(radians(CAST(l.longitude AS double precision)) - radians(CAST(:lng AS double precision)))
                      + sin(radians(CAST(:lat AS double precision))) * sin(radians(CAST(l.latitude AS double precision)))
                    ))
                  ) <= :radiusKm
            ORDER BY 6371 * acos(
                    LEAST(1.0, GREATEST(-1.0,
                      cos(radians(CAST(:lat AS double precision))) * cos(radians(CAST(l.latitude AS double precision))) *
                      cos(radians(CAST(l.longitude AS double precision)) - radians(CAST(:lng AS double precision)))
                      + sin(radians(CAST(:lat AS double precision))) * sin(radians(CAST(l.latitude AS double precision)))
                    ))
                  ) ASC
        """,
        countQuery = """
            SELECT count(*)
            FROM listings l
            JOIN listing_statuses ls ON ls.id = l.status_id
            JOIN pets p ON p.id = l.pet_id
            JOIN pet_species ps ON ps.id = p.species_id
            WHERE l.deleted_at IS NULL
              AND ls.code = 'ACTIVE'
              AND l.latitude IS NOT NULL
              AND l.longitude IS NOT NULL
              AND (:speciesCode IS NULL OR ps.code = :speciesCode)
              AND 6371 * acos(
                    LEAST(1.0, GREATEST(-1.0,
                      cos(radians(CAST(:lat AS double precision))) * cos(radians(CAST(l.latitude AS double precision))) *
                      cos(radians(CAST(l.longitude AS double precision)) - radians(CAST(:lng AS double precision)))
                      + sin(radians(CAST(:lat AS double precision))) * sin(radians(CAST(l.latitude AS double precision)))
                    ))
                  ) <= :radiusKm
        """,
        nativeQuery = true
    )
    fun findNearby(
        @Param("lat") lat: Double,
        @Param("lng") lng: Double,
        @Param("radiusKm") radiusKm: Double,
        @Param("speciesCode") speciesCode: String?,
        pageable: Pageable
    ): Page<Listing>
}