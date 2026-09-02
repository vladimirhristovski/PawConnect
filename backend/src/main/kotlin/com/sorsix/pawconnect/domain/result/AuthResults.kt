package com.sorsix.pawconnect.domain.result

import com.sorsix.pawconnect.domain.User
import com.sorsix.pawconnect.dto.response.AuthResponse

sealed interface RegisterResult {
    data class Success(val user: User) : RegisterResult
    data class Conflict(val message: String) : RegisterResult
}

sealed interface RefreshTokenResult {
    data class Success(val response: AuthResponse) : RefreshTokenResult
    data class Invalid(val message: String) : RefreshTokenResult
}

sealed interface ResetPasswordResult {
    data object Success : ResetPasswordResult
    data class Invalid(val message: String) : ResetPasswordResult
}

sealed interface DeleteOwnAccountResult {
    data object Success : DeleteOwnAccountResult
    data class NotFound(val message: String) : DeleteOwnAccountResult
}
