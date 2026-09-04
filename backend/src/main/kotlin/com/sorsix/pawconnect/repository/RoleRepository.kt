package com.sorsix.pawconnect.repository

import com.sorsix.pawconnect.domain.Role
import org.springframework.data.jpa.repository.JpaRepository

interface RoleRepository : JpaRepository<Role, Long> {
    fun findByName(name: String): Role?
}