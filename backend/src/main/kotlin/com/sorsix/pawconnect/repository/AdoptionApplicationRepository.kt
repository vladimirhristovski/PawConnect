package com.sorsix.pawconnect.repository

import com.sorsix.pawconnect.domain.AdoptionApplication
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface AdoptionApplicationRepository : JpaRepository<AdoptionApplication, Long> {

    companion object {
        private const val ASSOCIATIONS_FETCH = """
            JOIN FETCH a.listing l
            JOIN FETCH l.pet p
            JOIN FETCH p.species
            JOIN FETCH l.municipality
            JOIN FETCH l.status
            JOIN FETCH a.applicant
            JOIN FETCH a.status
            LEFT JOIN FETCH a.reviewedBy
        """
    }

    @Query("SELECT a FROM AdoptionApplication a $ASSOCIATIONS_FETCH WHERE a.id = :id")
    fun findByIdWithAllAssociations(@Param("id") id: Long): AdoptionApplication?

    @Query(
        """
        SELECT a FROM AdoptionApplication a $ASSOCIATIONS_FETCH
        WHERE a.applicant.id = :applicantId
        AND a.deletedAt IS NULL
        """
    )
    fun findByApplicant_IdAndDeletedAtIsNull(
        @Param("applicantId") applicantId: Long,
        pageable: Pageable
    ): Page<AdoptionApplication>

    @Query(
        """
        SELECT a FROM AdoptionApplication a $ASSOCIATIONS_FETCH
        WHERE a.listing.id = :listingId
        AND a.deletedAt IS NULL
        """
    )
    fun findByListing_IdAndDeletedAtIsNull(
        @Param("listingId") listingId: Long,
        pageable: Pageable
    ): Page<AdoptionApplication>

    @Query(
        """
        SELECT a FROM AdoptionApplication a $ASSOCIATIONS_FETCH
        WHERE (:statusCode IS NULL OR a.status.code = :statusCode)
        AND a.deletedAt IS NULL
        """
    )
    fun findAllWithAssociations(
        @Param("statusCode") statusCode: String?,
        pageable: Pageable
    ): Page<AdoptionApplication>

    // Simple existence checks (no FETCH needed)
    fun findByListing_IdAndApplicant_IdAndStatus_CodeInAndDeletedAtIsNull(
        listingId: Long,
        applicantId: Long,
        statusCodes: Collection<String>
    ): List<AdoptionApplication>

    fun findByListing_IdAndStatus_CodeInAndDeletedAtIsNull(
        listingId: Long,
        statusCodes: Collection<String>
    ): List<AdoptionApplication>
}