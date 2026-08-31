package com.sorsix.pawconnect.service

import com.sorsix.pawconnect.domain.User
import com.sorsix.pawconnect.repository.RefreshTokenRepository
import com.sorsix.pawconnect.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.Instant
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class UserServiceTest {

    private val userRepository = mockk<UserRepository>()
    private val refreshTokenRepository = mockk<RefreshTokenRepository>(relaxed = true)
    private lateinit var service: UserService

    @BeforeEach
    fun setup() {
        service = UserService(userRepository, refreshTokenRepository)
    }

    private fun mockUser(id: Long = 1L, deleted: Boolean = false): User {
        val user = mockk<User>(relaxed = true)
        every { user.id } returns id
        every { user.deletedAt } returns if (deleted) Instant.now() else null
        return user
    }

    @Test
    fun `setActive throws when the user does not exist`() {
        every { userRepository.findById(9L) } returns Optional.empty()
        assertNull(service.setActive(9L, false))
    }

    @Test
    fun `setActive deactivating a user revokes their refresh tokens`() {
        val user = mockUser(id = 5L)
        every { userRepository.findById(5L) } returns Optional.of(user)
        every { userRepository.save(user) } returns user

        service.setActive(5L, false)

        verify { user.isActive = false }
        verify { refreshTokenRepository.revokeAllUserTokens(5L, any()) }
    }

    @Test
    fun `setActive reactivating a user does not revoke tokens`() {
        val user = mockUser(id = 5L)
        every { userRepository.findById(5L) } returns Optional.of(user)
        every { userRepository.save(user) } returns user

        service.setActive(5L, true)

        verify { user.isActive = true }
        verify(exactly = 0) { refreshTokenRepository.revokeAllUserTokens(any(), any()) }
    }

    @Test
    fun `deleteUser throws when the user does not exist`() {
        every { userRepository.findById(9L) } returns Optional.empty()
        assertFalse(service.deleteUser(9L))
    }

    @Test
    fun `deleteUser is a no-op when the user is already soft-deleted`() {
        every { userRepository.findById(5L) } returns Optional.of(mockUser(id = 5L, deleted = true))
        service.deleteUser(5L)
        verify(exactly = 0) { userRepository.save(any()) }
        verify(exactly = 0) { refreshTokenRepository.revokeAllUserTokens(any(), any()) }
    }

    @Test
    fun `deleteUser soft-deletes the user and revokes their tokens`() {
        val user = mockUser(id = 5L)
        every { userRepository.findById(5L) } returns Optional.of(user)
        every { userRepository.save(user) } returns user

        service.deleteUser(5L)

        verify { user.deletedAt = any() }
        verify { userRepository.save(user) }
        verify { refreshTokenRepository.revokeAllUserTokens(5L, any()) }
    }

    @Test
    fun `searchUsers returns users in the order given by the id page`() {
        val pageable = PageRequest.of(0, 10)
        every { userRepository.searchUserIds(true, "ADMIN", pageable) } returns PageImpl(listOf(2L, 1L), pageable, 2L)
        every { userRepository.findAllByIdInWithRoles(listOf(2L, 1L)) } returns listOf(mockUser(id = 1L), mockUser(id = 2L))

        val result = service.searchUsers(true, "ADMIN", pageable)

        verify { userRepository.findAllByIdInWithRoles(listOf(2L, 1L)) }
        assertEquals(listOf(2L, 1L), result.content.map { it.id })
    }
}
