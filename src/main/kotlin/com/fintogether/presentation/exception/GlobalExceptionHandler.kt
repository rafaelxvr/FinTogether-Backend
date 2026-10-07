package com.fintogether.presentation.exception

import com.fintogether.application.dto.ApiResponse
import com.fintogether.domain.exception.*
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    // ==================== Custom Domain Exceptions ====================

    @ExceptionHandler(EntityNotFoundException::class)
    fun handleEntityNotFound(ex: EntityNotFoundException): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn("Entity not found: ${ex.entityType} - ${ex.message}")
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ApiResponse(success = false, message = ex.message))
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(ex: AccessDeniedException): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn("Access denied: ${ex.resourceType ?: "resource"} - ${ex.message}")
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(ApiResponse(success = false, message = ex.message))
    }

    @ExceptionHandler(RateLimitExceededException::class)
    fun handleRateLimitExceeded(ex: RateLimitExceededException): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn("Rate limit exceeded: ${ex.message}")
        val response = ResponseEntity
            .status(HttpStatus.TOO_MANY_REQUESTS)
            .header("Retry-After", ex.retryAfterSeconds?.toString() ?: "60")
        return response.body(ApiResponse(success = false, message = ex.message))
    }

    @ExceptionHandler(InvalidOperationException::class)
    fun handleInvalidOperation(ex: InvalidOperationException): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn("Invalid operation${ex.operation?.let { ": $it" } ?: ""} - ${ex.message}")
        return ResponseEntity
            .badRequest()
            .body(ApiResponse(success = false, message = ex.message))
    }

    @ExceptionHandler(ValidationException::class)
    fun handleValidation(ex: ValidationException): ResponseEntity<ApiResponse<Map<String, String?>>> {
        logger.warn("Validation failed: ${ex.field} - ${ex.message}")
        val errors = ex.field?.let { mapOf(it to ex.message) }
        return ResponseEntity
            .badRequest()
            .body(ApiResponse(success = false, message = ex.message, data = errors))
    }

    @ExceptionHandler(BusinessRuleException::class)
    fun handleBusinessRule(ex: BusinessRuleException): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn("Business rule violation${ex.rule?.let { ": $it" } ?: ""} - ${ex.message}")
        return ResponseEntity
            .status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(ApiResponse(success = false, message = ex.message))
    }

    @ExceptionHandler(EmailSendingException::class)
    fun handleEmailSending(ex: EmailSendingException): ResponseEntity<ApiResponse<Nothing>> {
        logger.error("Email sending failed: ${ex.message}", ex.cause)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse(success = false, message = "Failed to send email. Please try again later."))
    }

    // ==================== Spring/Standard Exceptions ====================

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn("Illegal argument: ${ex.message}")
        return ResponseEntity
            .badRequest()
            .body(ApiResponse(success = false, message = ex.message))
    }

    @ExceptionHandler(BadCredentialsException::class)
    fun handleBadCredentials(ex: BadCredentialsException): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn("Authentication failed: Invalid credentials")
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse(success = false, message = "Invalid email or password"))
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException::class)
    fun handleSpringAccessDenied(ex: org.springframework.security.access.AccessDeniedException): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn("Spring Security access denied: ${ex.message}")
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(ApiResponse(success = false, message = "You don't have permission to access this resource"))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationErrors(ex: MethodArgumentNotValidException): ResponseEntity<ApiResponse<Map<String, String>>> {
        val errors = ex.bindingResult.allErrors.associate { error ->
            val fieldName = (error as FieldError).field
            val errorMessage = error.defaultMessage ?: "Invalid value"
            fieldName to errorMessage
        }
        logger.warn("Validation errors: $errors")

        return ResponseEntity
            .badRequest()
            .body(ApiResponse(success = false, message = "Validation failed", data = errors))
    }

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrityViolation(ex: DataIntegrityViolationException): ResponseEntity<ApiResponse<Nothing>> {
        logger.error("Data integrity violation: ${ex.message}")
        val message = when {
            ex.message?.contains("unique constraint", ignoreCase = true) == true ->
                "A record with this data already exists"
            ex.message?.contains("foreign key", ignoreCase = true) == true ->
                "Cannot perform this operation due to related data"
            else -> "Data integrity error occurred"
        }
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ApiResponse(success = false, message = message))
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<ApiResponse<Nothing>> {
        logger.error("Unhandled exception: ${ex.javaClass.simpleName} - ${ex.message}", ex)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse(success = false, message = "An unexpected error occurred. Please try again later."))
    }
}
