package com.sorsix.pawconnect.domain.result

import com.sorsix.pawconnect.domain.Pet
import com.sorsix.pawconnect.domain.PetPhoto

sealed interface CreatePetResult {
    data class Success(val pet: Pet) : CreatePetResult
    data class NotFound(val message: String) : CreatePetResult
}

sealed interface UpdatePetResult {
    data class Success(val pet: Pet) : UpdatePetResult
    data class NotFound(val message: String) : UpdatePetResult
    data class Forbidden(val message: String) : UpdatePetResult
}

sealed interface AddPetPhotoResult {
    data class Success(val photo: PetPhoto) : AddPetPhotoResult
    data class NotFound(val message: String) : AddPetPhotoResult
    data class Forbidden(val message: String) : AddPetPhotoResult
}

sealed interface RemovePetPhotoResult {
    data object Success : RemovePetPhotoResult
    data class NotFound(val message: String) : RemovePetPhotoResult
    data class Forbidden(val message: String) : RemovePetPhotoResult
}
