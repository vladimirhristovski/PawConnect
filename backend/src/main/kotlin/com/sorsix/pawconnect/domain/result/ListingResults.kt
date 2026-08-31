package com.sorsix.pawconnect.domain.result

import com.sorsix.pawconnect.domain.Listing

sealed interface CreateListingResult {
    data class Success(val listing: Listing) : CreateListingResult
    data class NotFound(val message: String) : CreateListingResult
    data class Conflict(val message: String) : CreateListingResult
    data class Forbidden(val message: String) : CreateListingResult
}

sealed interface PublishListingResult {
    data class Success(val listing: Listing) : PublishListingResult
    data class NotFound(val message: String) : PublishListingResult
    data class Conflict(val message: String) : PublishListingResult
    data class Forbidden(val message: String) : PublishListingResult
}

sealed interface UpdateListingResult {
    data class Success(val listing: Listing) : UpdateListingResult
    data class NotFound(val message: String) : UpdateListingResult
    data class Conflict(val message: String) : UpdateListingResult
    data class Forbidden(val message: String) : UpdateListingResult
}

sealed interface CancelListingResult {
    data class Success(val listing: Listing) : CancelListingResult
    data class NotFound(val message: String) : CancelListingResult
    data class Conflict(val message: String) : CancelListingResult
    data class Forbidden(val message: String) : CancelListingResult
}

sealed interface DeleteListingResult {
    data object Success : DeleteListingResult
    data class NotFound(val message: String) : DeleteListingResult
    data class Forbidden(val message: String) : DeleteListingResult
}
