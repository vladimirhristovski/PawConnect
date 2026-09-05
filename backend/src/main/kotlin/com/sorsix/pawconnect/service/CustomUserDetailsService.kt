package com.sorsix.pawconnect.security

import com.sorsix.pawconnect.repository.UserRepository
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class CustomUserDetailsService(
    private val userRepository: UserRepository,
) : UserDetailsService {
    override fun loadUserByUsername(username: String): UserDetails {
        val user =
            userRepository.findByUsernameActive(username)
                ?: throw UsernameNotFoundException("User not found")
        return CustomUserDetails(user)
    }
}
