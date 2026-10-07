package com.fintogether.domain.exception

/**
 * Base exception for all FinTogether domain exceptions.
 * Provides a consistent exception hierarchy for the application.
 */
abstract class FinTogetherException(
    override val message: String,
    override val cause: Throwable? = null
) : RuntimeException(message, cause)

/**
 * Thrown when a requested entity is not found in the database.
 */
class EntityNotFoundException(
    val entityType: String,
    val entityId: Any? = null,
    message: String = if (entityId != null) "$entityType with id '$entityId' not found" else "$entityType not found"
) : FinTogetherException(message)

/**
 * Thrown when a user attempts to access a resource they don't have permission to access.
 */
class AccessDeniedException(
    message: String = "You don't have permission to access this resource",
    val resourceType: String? = null,
    val resourceId: Any? = null
) : FinTogetherException(message)

/**
 * Thrown when a rate limit has been exceeded.
 */
class RateLimitExceededException(
    message: String = "Too many requests. Please try again later.",
    val retryAfterSeconds: Long? = null
) : FinTogetherException(message)

/**
 * Thrown when an operation is invalid given the current state.
 */
class InvalidOperationException(
    message: String,
    val operation: String? = null
) : FinTogetherException(message)

/**
 * Thrown when input validation fails.
 */
class ValidationException(
    message: String,
    val field: String? = null,
    val rejectedValue: Any? = null
) : FinTogetherException(message)

/**
 * Thrown when a business rule is violated.
 */
class BusinessRuleException(
    message: String,
    val rule: String? = null
) : FinTogetherException(message)

/**
 * Thrown when email sending fails.
 */
class EmailSendingException(
    message: String = "Failed to send email",
    cause: Throwable? = null
) : FinTogetherException(message, cause)
