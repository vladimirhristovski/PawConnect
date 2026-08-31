package com.sorsix.pawconnect.service

import com.sorsix.pawconnect.dto.response.UserResponse
import com.sorsix.pawconnect.repository.RefreshTokenRepository
import com.sorsix.pawconnect.repository.UserRepository
import com.sorsix.pawconnect.common.requireId
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class UserService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional(readOnly = true)
    fun searchUsers(active: Boolean?, role: String?, pageable: Pageable): Page<UserResponse> {
        val idPage = userRepository.searchUserIds(active, role, pageable)
        val usersById = userRepository.findAllByIdInWithRoles(idPage.content).associateBy { it.id }
        val users = idPage.content.mapNotNull { usersById[it] }.map { UserResponse.from(it) }
        return PageImpl(users, pageable, idPage.totalElements)
    }

    @Transactional
    fun setActive(id: Long, active: Boolean): UserResponse? {
        val user = userRepository.findById(id).orElse(null) ?: return null
        user.isActive = active
        val updated = userRepository.save(user)
        if (!active) {
            refreshTokenRepository.revokeAllUserTokens(user.requireId(), Instant.now())
        }
        log.info("User {} active status set to {}", updated.id, active)
        return UserResponse.from(updated)
    }

    @Transactional
    fun deleteUser(id: Long): Boolean {
        val user = userRepository.findById(id).orElse(null) ?: return false
        if (user.deletedAt != null) return true
        user.deletedAt = Instant.now()
        userRepository.save(user)
        refreshTokenRepository.revokeAllUserTokens(user.requireId(), Instant.now())
        log.info("User {} soft-deleted", user.id)
        return true
    }
}
