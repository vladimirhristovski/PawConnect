package com.sorsix.pawconnect.repository

import com.sorsix.pawconnect.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional

interface UserRepository : JpaRepository<User, Long> {
    @Query("SELECT u FROM User u WHERE u.username = :username AND u.deletedAt IS NULL")
    fun findByUsernameActive(@Param("username") username: String): Optional<User>

    @Query("SELECT u FROM User u WHERE u.email = :email AND u.deletedAt IS NULL")
    fun findByEmailActive(@Param("email") email: String): Optional<User>

    fun existsByUsernameAndDeletedAtIsNull(username: String): Boolean
    fun existsByEmailAndDeletedAtIsNull(email: String): Boolean
}