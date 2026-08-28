package com.sorsix.pawconnect.repository

import com.sorsix.pawconnect.domain.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.*

interface UserRepository : JpaRepository<User, Long> {
    @Query("SELECT u FROM User u WHERE u.username = :username AND u.deletedAt IS NULL")
    fun findByUsernameActive(@Param("username") username: String): Optional<User>

    @Query("SELECT u FROM User u WHERE u.email = :email AND u.deletedAt IS NULL")
    fun findByEmailActive(@Param("email") email: String): Optional<User>

    fun existsByUsernameAndDeletedAtIsNull(username: String): Boolean
    fun existsByEmailAndDeletedAtIsNull(email: String): Boolean

    @Query(
        """
        SELECT DISTINCT u.id FROM User u
        LEFT JOIN u.roles r
        WHERE (:active IS NULL OR u.isActive = :active)
        AND (:role IS NULL OR r.name = :role)
        """
    )
    fun searchUserIds(
        @Param("active") active: Boolean?,
        @Param("role") role: String?,
        pageable: Pageable
    ): Page<Long>

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.roles WHERE u.id IN :ids")
    fun findAllByIdInWithRoles(@Param("ids") ids: List<Long>): List<User>
}