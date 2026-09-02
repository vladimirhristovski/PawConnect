package com.sorsix.pawconnect.api

import com.sorsix.pawconnect.common.problemResponse
import com.sorsix.pawconnect.domain.result.DeleteOwnAccountResult
import com.sorsix.pawconnect.domain.result.RefreshTokenResult
import com.sorsix.pawconnect.domain.result.RegisterResult
import com.sorsix.pawconnect.domain.result.ResetPasswordResult
import com.sorsix.pawconnect.dto.request.*
import com.sorsix.pawconnect.dto.response.AuthResponse
import com.sorsix.pawconnect.dto.response.UserResponse
import com.sorsix.pawconnect.exception.UnauthorizedException
import com.sorsix.pawconnect.service.AuthService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<*> {
        return when (val result = authService.register(request)) {
            is RegisterResult.Success -> ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(result.user))
            is RegisterResult.Conflict -> problemResponse(HttpStatus.CONFLICT, result.message)
        }
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<AuthResponse> {
        val response = authService.login(request)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody request: RefreshTokenRequest): ResponseEntity<*> {
        return when (val result = authService.refreshToken(request.refreshToken)) {
            is RefreshTokenResult.Success -> ResponseEntity.ok(result.response)
            is RefreshTokenResult.Invalid -> problemResponse(HttpStatus.BAD_REQUEST, result.message)
        }
    }

    @PostMapping("/logout")
    fun logout(@Valid @RequestBody request: RefreshTokenRequest): ResponseEntity<Void> {
        authService.logout(request.refreshToken)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/forgot-password")
    fun forgotPassword(@Valid @RequestBody request: ForgotPasswordRequest): ResponseEntity<Void> {
        authService.forgotPassword(request)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/reset-password")
    fun resetPassword(@Valid @RequestBody request: ResetPasswordRequest): ResponseEntity<*> {
        return when (val result = authService.resetPassword(request)) {
            is ResetPasswordResult.Success -> ResponseEntity.ok().build<Unit>()
            is ResetPasswordResult.Invalid -> problemResponse(HttpStatus.BAD_REQUEST, result.message)
        }
    }

    @GetMapping("/me")
    fun me(): ResponseEntity<UserResponse> {
        val user = authService.getCurrentUserResponse()
            ?: throw UnauthorizedException("Not authenticated")
        return ResponseEntity.ok(user)
    }

    @DeleteMapping("/me")
    fun deleteAccount(): ResponseEntity<*> {
        val currentUser = authService.getCurrentUser() ?: throw UnauthorizedException("Not authenticated")
        return when (val result = authService.deleteOwnAccount(currentUser)) {
            is DeleteOwnAccountResult.Success -> ResponseEntity.noContent().build<Unit>()
            is DeleteOwnAccountResult.NotFound -> problemResponse(HttpStatus.NOT_FOUND, result.message)
        }
    }
}