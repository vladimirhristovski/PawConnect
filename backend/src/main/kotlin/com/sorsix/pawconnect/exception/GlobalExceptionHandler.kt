package com.sorsix.pawconnect.exception

import com.sorsix.pawconnect.common.problemResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.DisabledException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.multipart.MaxUploadSizeExceededException

@RestControllerAdvice
class GlobalExceptionHandler {
    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ResponseEntity<ProblemDetail> =
        problemResponse(HttpStatus.BAD_REQUEST, ex.message ?: "Invalid request")

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(ex: MethodArgumentTypeMismatchException): ResponseEntity<ProblemDetail> {
        val message =
            "Failed to convert parameter '${ex.name}' with value '${ex.value}' to required type '${ex.requiredType?.simpleName}'"
        return problemResponse(
            HttpStatus.BAD_REQUEST, message,
            mapOf("parameter" to ex.name, "value" to ex.value)
        )
    }

    @ExceptionHandler(BadCredentialsException::class)
    fun handleBadCredentials(ex: BadCredentialsException): ResponseEntity<ProblemDetail> =
        problemResponse(HttpStatus.UNAUTHORIZED, "Invalid username or password")

    @ExceptionHandler(DisabledException::class)
    fun handleDisabled(ex: DisabledException): ResponseEntity<ProblemDetail> =
        problemResponse(HttpStatus.UNAUTHORIZED, "Account is deactivated")

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ProblemDetail> {
        val errors = ex.bindingResult.allErrors.map {
            if (it is FieldError) mapOf("field" to it.field, "message" to it.defaultMessage)
            else mapOf("message" to it.defaultMessage)
        }
        return problemResponse(HttpStatus.BAD_REQUEST, "Validation failed", mapOf("errors" to errors))
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadable(ex: HttpMessageNotReadableException): ResponseEntity<ProblemDetail> =
        problemResponse(HttpStatus.BAD_REQUEST, "Malformed JSON request")

    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun handleMaxUploadSize(ex: MaxUploadSizeExceededException): ResponseEntity<ProblemDetail> =
        problemResponse(HttpStatus.PAYLOAD_TOO_LARGE, "Uploaded file exceeds the maximum allowed size")

    @ExceptionHandler(BlobStorageException::class)
    fun handleBlobStorage(ex: BlobStorageException): ResponseEntity<ProblemDetail> {
        log.error("Blob storage operation failed", ex)
        return problemResponse(HttpStatus.BAD_GATEWAY, ex.message ?: "File storage error")
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneric(ex: Exception): ResponseEntity<ProblemDetail> {
        log.error("Unexpected error", ex)
        return problemResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred")
    }

    @ExceptionHandler(UnauthorizedException::class)
    fun handleUnauthorized(ex: UnauthorizedException): ResponseEntity<ProblemDetail> =
        problemResponse(HttpStatus.UNAUTHORIZED, ex.message ?: "Not authenticated")
}
