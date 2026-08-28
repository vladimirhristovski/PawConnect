package com.sorsix.pawconnect.dto.response

import com.sorsix.pawconnect.domain.User
import com.sorsix.pawconnect.common.requireId

data class UserResponse(
    val id: Long,
    val username: String,
    val email: String,
    val firstName: String?,
    val lastName: String?,
    val phone: String?,
    val roles: List<String>,
    val isActive: Boolean
) {
    companion object {
        fun from(user: User): UserResponse {
            return UserResponse(
                id = user.requireId(),
                username = user.username,
                email = user.email,
                firstName = user.firstName,
                lastName = user.lastName,
                phone = user.phone,
                roles = user.roles.map { it.name },
                isActive = user.isActive
            )
        }
    }
}