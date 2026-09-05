package com.sorsix.pawconnect.exception

class BlobStorageException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
