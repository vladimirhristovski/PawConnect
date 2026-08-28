package com.sorsix.pawconnect.exception

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
import org.springframework.web.context.request.WebRequest
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.multipart.MaxUploadSizeExceededException
import java.net.URI

@RestControllerAdvice
class GlobalExceptionHandler {
    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    private fun problem(
        status: HttpStatus,
        detail: String,
        request: WebRequest,
        properties: Map<String, Any?> = emptyMap()
    ): ResponseEntity<ProblemDetail> {
        val pd = ProblemDetail.forStatusAndDetail(status, detail)
        pd.type = URI.create("about:blank")
        pd.instance = URI.create(request.getDescription(false))
        properties.forEach { (key, value) -> pd.setProperty(key, value) }
        return ResponseEntity.status(status).body(pd)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException, request: WebRequest): ResponseEntity<ProblemDetail> =
        problem(HttpStatus.BAD_REQUEST, ex.message ?: "Invalid request", request)

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(
        ex: MethodArgumentTypeMismatchException, request: WebRequest
    ): ResponseEntity<ProblemDetail> {
        val message =
            "Failed to convert parameter '${ex.name}' with value '${ex.value}' to required type '${ex.requiredType?.simpleName}'"
        return problem(
            HttpStatus.BAD_REQUEST, message, request,
            mapOf("parameter" to ex.name, "value" to ex.value)
        )
    }

    @ExceptionHandler(BadCredentialsException::class)
    fun handleBadCredentials(ex: BadCredentialsException, request: WebRequest): ResponseEntity<ProblemDetail> =
        problem(HttpStatus.UNAUTHORIZED, "Invalid username or password", request)

    @ExceptionHandler(DisabledException::class)
    fun handleDisabled(ex: DisabledException, request: WebRequest): ResponseEntity<ProblemDetail> =
        problem(HttpStatus.UNAUTHORIZED, "Account is deactivated", request)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException, request: WebRequest): ResponseEntity<ProblemDetail> {
        val errors = ex.bindingResult.allErrors.map {
            if (it is FieldError) mapOf("field" to it.field, "message" to it.defaultMessage)
            else mapOf("message" to it.defaultMessage)
        }
        return problem(HttpStatus.BAD_REQUEST, "Validation failed", request, mapOf("errors" to errors))
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadable(
        ex: HttpMessageNotReadableException, request: WebRequest
    ): ResponseEntity<ProblemDetail> =
        problem(HttpStatus.BAD_REQUEST, "Malformed JSON request", request)

    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun handleMaxUploadSize(
        ex: MaxUploadSizeExceededException, request: WebRequest
    ): ResponseEntity<ProblemDetail> =
        problem(HttpStatus.PAYLOAD_TOO_LARGE, "Uploaded file exceeds the maximum allowed size", request)

    @ExceptionHandler(BlobStorageException::class)
    fun handleBlobStorage(ex: BlobStorageException, request: WebRequest): ResponseEntity<ProblemDetail> {
        log.error("Blob storage operation failed", ex)
        return problem(HttpStatus.BAD_GATEWAY, ex.message ?: "File storage error", request)
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneric(ex: Exception, request: WebRequest): ResponseEntity<ProblemDetail> {
        log.error("Unexpected error", ex)
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request)
    }

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleResourceNotFound(ex: ResourceNotFoundException, request: WebRequest): ResponseEntity<ProblemDetail> =
        problem(HttpStatus.NOT_FOUND, ex.message ?: "Resource not found", request)

    @ExceptionHandler(ForbiddenOperationException::class)
    fun handleForbidden(ex: ForbiddenOperationException, request: WebRequest): ResponseEntity<ProblemDetail> =
        problem(HttpStatus.FORBIDDEN, ex.message ?: "Access denied", request)

    @ExceptionHandler(ConflictException::class)
    fun handleConflict(ex: ConflictException, request: WebRequest): ResponseEntity<ProblemDetail> =
        problem(HttpStatus.CONFLICT, ex.message ?: "Conflict", request)

    @ExceptionHandler(UnauthorizedException::class)
    fun handleUnauthorized(ex: UnauthorizedException, request: WebRequest): ResponseEntity<ProblemDetail> =
        problem(HttpStatus.UNAUTHORIZED, ex.message ?: "Not authenticated", request)
}
