package com.sorsix.pawconnect.service

import com.sorsix.pawconnect.dto.request.*
import com.sorsix.pawconnect.dto.response.AuthResponse
import com.sorsix.pawconnect.dto.response.UserResponse
import com.sorsix.pawconnect.exception.UnauthorizedException
import com.sorsix.pawconnect.model.PasswordResetToken
import com.sorsix.pawconnect.model.User
import com.sorsix.pawconnect.repository.PasswordResetTokenRepository
import com.sorsix.pawconnect.repository.RefreshTokenRepository
import com.sorsix.pawconnect.repository.RoleRepository
import com.sorsix.pawconnect.repository.UserRepository
import com.sorsix.pawconnect.security.CustomUserDetails
import com.sorsix.pawconnect.security.JwtService
import com.sorsix.pawconnect.util.requireId
import jakarta.mail.internet.MimeMessage
import org.hibernate.exception.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import java.util.*

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val roleRepository: RoleRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val authenticationManager: AuthenticationManager,
    private val passwordResetTokenRepository: PasswordResetTokenRepository,
    private val emailService: EmailService,
    private val userService: UserService,
    @Value("\${app.jwt.access-token-ttl}") private val accessTokenTtl: Long,
    @Value("\${app.reset-token-ttl}") private val resetTokenTtl: Long,
    @Value("\${app.frontend-url}") private val frontendUrl: String
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val encodedHash = digest.digest(token.toByteArray(StandardCharsets.UTF_8))
        return HexFormat.of().formatHex(encodedHash)
    }

    @Transactional
    fun register(request: RegisterRequest): UserResponse {
        if (userRepository.existsByUsernameAndDeletedAtIsNull(request.username)) {
            throw IllegalArgumentException("Username already taken")
        }
        if (userRepository.existsByEmailAndDeletedAtIsNull(request.email)) {
            throw IllegalArgumentException("Email already registered")
        }

        val user = User(
            username = request.username,
            email = request.email,
            password = passwordEncoder.encode(request.password)
                ?: throw IllegalStateException("Password encoder returned null"),
            firstName = request.firstName,
            lastName = request.lastName,
            phone = request.phone
        )
        val userRole = roleRepository.findByName("USER")
            .orElseThrow { IllegalStateException("USER role not found") }
        user.roles.add(userRole)

        val saved = try {
            userRepository.save(user)
        } catch (ex: DataIntegrityViolationException) {
            val constraintName = (ex.cause as? ConstraintViolationException)?.constraintName
            val message = when (constraintName) {
                "uq_users_username_active" -> "Username already taken"
                "uq_users_email_active" -> "Email already registered"
                else -> "Username or email already registered"
            }
            throw IllegalArgumentException(message)
        }
        log.info("User registered: {}", saved.id)
        return UserResponse.from(saved)
    }

    @Transactional
    fun login(request: LoginRequest): AuthResponse {
        val auth = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(request.username, request.password)
        )
        val user = (auth.principal as CustomUserDetails).getUser()

        refreshTokenRepository.revokeAllUserTokens(user.requireId(), Instant.now())

        val accessToken = jwtService.generateAccessToken(auth.principal as UserDetails)
        val (rawRefresh, refreshEntity) = jwtService.generateRefreshToken(user)
        refreshTokenRepository.save(refreshEntity)

        log.info("User logged in: {}", user.id)

        return AuthResponse(
            accessToken = accessToken,
            refreshToken = rawRefresh,
            expiresIn = accessTokenTtl / 1000
        )
    }

    @Transactional
    fun refreshToken(refreshToken: String): AuthResponse {
        val rt = jwtService.verifyRefreshToken(refreshToken)
            ?: throw IllegalArgumentException("Invalid or expired refresh token")

        val user = rt.user
        rt.revokedAt = Instant.now()
        refreshTokenRepository.save(rt)

        val userDetails = CustomUserDetails(user)
        val newAccess = jwtService.generateAccessToken(userDetails)
        val (newRawRefresh, newRefreshEntity) = jwtService.generateRefreshToken(user)
        refreshTokenRepository.save(newRefreshEntity)

        log.info("Refresh token rotated for user {}", user.id)

        return AuthResponse(
            accessToken = newAccess,
            refreshToken = newRawRefresh,
            expiresIn = accessTokenTtl / 1000
        )
    }

    @Transactional
    fun logout(refreshToken: String): Boolean {
        val revoked = jwtService.revokeRefreshToken(refreshToken)
        log.info("Logout requested; refresh token revoked: {}", revoked)
        return revoked
    }

    @Transactional
    fun forgotPassword(request: ForgotPasswordRequest): Boolean {
        val user = userRepository.findByEmailActive(request.email).orElse(null)
        if (user == null) {
            return true
        }

        passwordResetTokenRepository.revokeAllUnusedTokensForUser(user.requireId(), Instant.now())

        val rawToken = UUID.randomUUID().toString() + System.currentTimeMillis()
        val tokenHash = hashToken(rawToken)
        val expiry = Instant.now().plusMillis(resetTokenTtl)

        val resetToken = PasswordResetToken(
            user = user,
            tokenHash = tokenHash,
            expiresAt = expiry
        )
        passwordResetTokenRepository.save(resetToken)

        log.info("Password reset requested for user {}", user.id)

        val resetLink = "$frontendUrl/reset-password?token=$rawToken"
        val emailBody = """
            You requested a password reset.
            Click the link below to set a new password:
            $resetLink
            
            If you did not request this, please ignore this email.
        """.trimIndent()

        emailService.sendEmail(user.email, "Password Reset Request", emailBody)

        return true
    }
    @Transactional
    fun resetPassword(request: ResetPasswordRequest): Boolean {
        val tokenHash = hashToken(request.token)
        val resetToken = passwordResetTokenRepository.findByTokenHash(tokenHash)
            .orElseThrow { IllegalArgumentException("Invalid token") }

        if (resetToken.usedAt != null) {
            throw IllegalArgumentException("Token already used")
        }
        if (resetToken.expiresAt.isBefore(Instant.now())) {
            throw IllegalArgumentException("Token expired")
        }

        val user = resetToken.user
        user.password = passwordEncoder.encode(request.newPassword)
            ?: throw IllegalStateException("Password encoder returned null")
        userRepository.save(user)

        resetToken.usedAt = Instant.now()
        passwordResetTokenRepository.save(resetToken)

        log.info("Password reset completed for user {}", user.id)

        return true
    }

    @Transactional
    fun deleteOwnAccount() {
        val user = getCurrentUser() ?: throw UnauthorizedException("Not authenticated")
        userService.deleteUser(user.requireId())
    }

    fun getCurrentUser(): User? {
        val authentication = SecurityContextHolder.getContext().authentication
        return if (authentication != null && authentication.isAuthenticated &&
            authentication.principal is CustomUserDetails
        ) {
            (authentication.principal as CustomUserDetails).getUser()
        } else null
    }

    fun getCurrentUserResponse(): UserResponse? {
        val user = getCurrentUser() ?: return null
        return UserResponse.from(user)
    }

    fun requireCurrentUser(): User {
        return getCurrentUser() ?: throw IllegalStateException("No authenticated user in security context")
    }
}