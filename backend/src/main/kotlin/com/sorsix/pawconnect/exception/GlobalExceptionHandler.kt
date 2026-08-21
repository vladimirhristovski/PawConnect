package com.sorsix.pawconnect.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import java.net.URI

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException, request: WebRequest): ResponseEntity<ProblemDetail> {
        val pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.message ?: "Invalid request")
        pd.type = URI.create("about:blank")
        pd.instance = URI.create(request.getDescription(false))
        return ResponseEntity.badRequest().body(pd)
    }

    @ExceptionHandler(BadCredentialsException::class)
    fun handleBadCredentials(ex: BadCredentialsException, request: WebRequest): ResponseEntity<ProblemDetail> {
        val pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid username or password")
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

    @ExceptionHandler(Exception::class)
    fun handleGeneric(ex: Exception, request: WebRequest): ResponseEntity<ProblemDetail> {
        ex.printStackTrace()
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
}