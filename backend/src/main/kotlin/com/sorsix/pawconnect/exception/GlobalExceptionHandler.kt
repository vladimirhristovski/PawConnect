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

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException, request: WebRequest): ResponseEntity<ProblemDetail> {
        val pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.message ?: "Invalid request")
        pd.type = URI.create("about:blank")
        pd.instance = URI.create(request.getDescription(false))
        return ResponseEntity.badRequest().body(pd)
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(
        ex: MethodArgumentTypeMismatchException, request: WebRequest
    ): ResponseEntity<ProblemDetail> {
        val message =
            "Failed to convert parameter '${ex.name}' with value '${ex.value}' to required type '${ex.requiredType?.simpleName}'"
        val pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, message)
        pd.type = URI.create("about:blank")
        pd.instance = URI.create(request.getDescription(false))
        pd.setProperty("parameter", ex.name)
        pd.setProperty("value", ex.value)
        return ResponseEntity.badRequest().body(pd)
    }

    @ExceptionHandler(BadCredentialsException::class)
    fun handleBadCredentials(ex: BadCredentialsException, request: WebRequest): ResponseEntity<ProblemDetail> {
        val pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid username or password")
        pd.type = URI.create("about:blank")
        pd.instance = URI.create(request.getDescription(false))
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(pd)
    }

    @ExceptionHandler(DisabledException::class)
    fun handleDisabled(ex: DisabledException, request: WebRequest): ResponseEntity<ProblemDetail> {
        val pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Account is deactivated")
        pd.type = URI.create("about:blank")
        pd.instance = URI.create(request.getDescription(false))
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(pd)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException, request: WebRequest): ResponseEntity<ProblemDetail> {
        val errors = ex.bindingResult.allErrors.map {
            if (it is FieldError) mapOf("field" to it.field, "message" to it.defaultMessage)
            else mapOf("message" to it.defaultMessage)
        }
        val pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed")
        pd.type = URI.create("about:blank")
        pd.instance = URI.create(request.getDescription(false))
        pd.setProperty("errors", errors)
        return ResponseEntity.badRequest().body(pd)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadable(
        ex: HttpMessageNotReadableException, request: WebRequest
    ): ResponseEntity<ProblemDetail> {
        val pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Malformed JSON request")
        pd.type = URI.create("about:blank")
        pd.instance = URI.create(request.getDescription(false))
        return ResponseEntity.badRequest().body(pd)
    }

    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun handleMaxUploadSize(
        ex: MaxUploadSizeExceededException, request: WebRequest
    ): ResponseEntity<ProblemDetail> {
        val pd = ProblemDetail.forStatusAndDetail(HttpStatus.PAYLOAD_TOO_LARGE, "Uploaded file exceeds the maximum allowed size")
        pd.type = URI.create("about:blank")
        pd.instance = URI.create(request.getDescription(false))
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(pd)
    }

    @ExceptionHandler(BlobStorageException::class)
    fun handleBlobStorage(ex: BlobStorageException, request: WebRequest): ResponseEntity<ProblemDetail> {
        log.error("Blob storage operation failed", ex)
        val pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, ex.message ?: "File storage error")
        pd.type = URI.create("about:blank")
        pd.instance = URI.create(request.getDescription(false))
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(pd)
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneric(ex: Exception, request: WebRequest): ResponseEntity<ProblemDetail> {
        log.error("Unexpected error", ex)
        val pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred")
        pd.type = URI.create("about:blank")
        pd.instance = URI.create(request.getDescription(false))
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(pd)
    }

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleResourceNotFound(ex: ResourceNotFoundException, request: WebRequest): ResponseEntity<ProblemDetail> {
        val pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.message ?: "Resource not found")
        pd.type = URI.create("about:blank")
        pd.instance = URI.create(request.getDescription(false))
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd)
    }

    @ExceptionHandler(ForbiddenOperationException::class)
    fun handleForbidden(ex: ForbiddenOperationException, request: WebRequest): ResponseEntity<ProblemDetail> {
        val pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.message ?: "Access denied")
        pd.type = URI.create("about:blank")
        pd.instance = URI.create(request.getDescription(false))
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(pd)
    }

    @ExceptionHandler(ConflictException::class)
    fun handleConflict(ex: ConflictException, request: WebRequest): ResponseEntity<ProblemDetail> {
        val pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.message ?: "Conflict")
        pd.type = URI.create("about:blank")
        pd.instance = URI.create(request.getDescription(false))
        return ResponseEntity.status(HttpStatus.CONFLICT).body(pd)
    }

    @ExceptionHandler(UnauthorizedException::class)
    fun handleUnauthorized(ex: UnauthorizedException, request: WebRequest): ResponseEntity<ProblemDetail> {
        val pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.message ?: "Not authenticated")
        pd.type = URI.create("about:blank")
        pd.instance = URI.create(request.getDescription(false))
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(pd)
    }
}