package com.sorsix.pawconnect.service

import com.sorsix.pawconnect.dto.response.UserResponse
import com.sorsix.pawconnect.exception.ResourceNotFoundException
import com.sorsix.pawconnect.repository.RefreshTokenRepository
import com.sorsix.pawconnect.repository.UserRepository
import com.sorsix.pawconnect.util.requireId
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
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
        val page = userRepository.searchUsers(active, role, pageable)
        return page.map { UserResponse.from(it) }
    }

    @Transactional
    fun setActive(id: Long, active: Boolean): UserResponse {
        val user = userRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("User not found: $id") }
        user.isActive = active
        val updated = userRepository.save(user)
        if (!active) {
            refreshTokenRepository.revokeAllUserTokens(user.requireId(), Instant.now())
        }
        log.info("User {} active status set to {}", updated.id, active)
        return UserResponse.from(updated)
    }

    @Transactional
    fun deleteUser(id: Long) {
        val user = userRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("User not found: $id") }
        if (user.deletedAt != null) return
        user.deletedAt = Instant.now()
        userRepository.save(user)
        refreshTokenRepository.revokeAllUserTokens(user.requireId(), Instant.now())
        log.info("User {} soft-deleted", user.id)
    }
}
