package com.sorsix.pawconnect.domain.result

import com.sorsix.pawconnect.dto.response.BusinessPhotoResponse
import com.sorsix.pawconnect.dto.response.BusinessResponse

sealed interface CreateBusinessResult {
    data class Success(val business: BusinessResponse) : CreateBusinessResult
    data class NotFound(val message: String) : CreateBusinessResult
}

sealed interface UpdateBusinessResult {
    data class Success(val business: BusinessResponse) : UpdateBusinessResult
    data class NotFound(val message: String) : UpdateBusinessResult
    data class Forbidden(val message: String) : UpdateBusinessResult
}

sealed interface DeleteBusinessResult {
    data object Success : DeleteBusinessResult
    data class NotFound(val message: String) : DeleteBusinessResult
    data class Forbidden(val message: String) : DeleteBusinessResult
}

sealed interface AddBusinessPhotoResult {
    data class Success(val photo: BusinessPhotoResponse) : AddBusinessPhotoResult
    data class NotFound(val message: String) : AddBusinessPhotoResult
    data class Forbidden(val message: String) : AddBusinessPhotoResult
}

sealed interface RemoveBusinessPhotoResult {
    data object Success : RemoveBusinessPhotoResult
    data class NotFound(val message: String) : RemoveBusinessPhotoResult
    data class Forbidden(val message: String) : RemoveBusinessPhotoResult
}
