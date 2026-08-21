package com.sorsix.pawconnect.repository

import com.sorsix.pawconnect.model.Business
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface BusinessRepository : JpaRepository<Business, Long>, JpaSpecificationExecutor<Business> {
    @Query("SELECT b FROM Business b JOIN FETCH b.type JOIN FETCH b.municipality LEFT JOIN FETCH b.owner WHERE b.id = :id")
    fun findByIdWithAssociations(@Param("id") id: Long): Business?
}