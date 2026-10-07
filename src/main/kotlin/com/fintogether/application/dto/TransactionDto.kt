package com.fintogether.application.dto

import com.fintogether.domain.entity.TransactionType
import com.fintogether.domain.entity.TransactionVisibility
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

// Re-export split DTOs for convenience
// CreateSplitRequest and SplitResponse are in TransactionSplitDto.kt

data class CreateTransactionRequest(
    @field:NotNull(message = "Transaction date is required")
    val transactionDate: LocalDate,

    @field:NotNull(message = "Amount is required")
    val amount: BigDecimal,

    @field:NotNull(message = "Transaction type is required")
    val type: TransactionType,

    val accountId: UUID? = null,

    val categoryId: UUID? = null,

    val cardId: UUID? = null,

    @field:Size(max = 500, message = "Description must not exceed 500 characters")
    val description: String? = null,

    @field:Size(max = 1000, message = "Notes must not exceed 1000 characters")
    val notes: String? = null,

    val tags: List<String>? = null,

    val visibility: TransactionVisibility = TransactionVisibility.SHARED,

    val isRecurring: Boolean = false,

    val recurrenceRule: Map<String, Any>? = null,

    val isInstallment: Boolean = false,

    val installmentGroupId: UUID? = null,

    val installmentNumber: Int? = null,

    val merchantId: UUID? = null,

    val splitWith: List<CreateSplitRequest>? = null
)

data class UpdateTransactionRequest(
    @field:NotNull(message = "Transaction date is required")
    val transactionDate: LocalDate,

    @field:NotNull(message = "Amount is required")
    val amount: BigDecimal,

    @field:Size(max = 500, message = "Description must not exceed 500 characters")
    val description: String? = null,

    @field:Size(max = 1000, message = "Notes must not exceed 1000 characters")
    val notes: String? = null,

    val tags: List<String>? = null,

    val categoryId: UUID? = null,

    val visibility: TransactionVisibility? = null,

    val merchantId: UUID? = null
)

data class TransactionResponse(
    val id: UUID,
    val budgetId: UUID,
    val userId: UUID?,
    val transactionDate: LocalDate,
    val amount: BigDecimal,
    val type: TransactionType,
    val accountId: UUID?,
    val categoryId: UUID?,
    val cardId: UUID?,
    val description: String?,
    val notes: String?,
    val tags: List<String>?,
    val visibility: TransactionVisibility,
    val isRecurring: Boolean,
    val recurrenceRule: Map<String, Any>?,
    val isInstallment: Boolean,
    val installmentGroupId: UUID?,
    val installmentNumber: Int?,
    val parentRecurringId: UUID?,
    val merchantId: UUID?,
    val syncedAt: LocalDateTime?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val splits: List<SplitResponse>? = null
)

data class TransactionFilterRequest(
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val type: TransactionType? = null,
    val categoryId: UUID? = null,
    val accountId: UUID? = null,
    val cardId: UUID? = null,
    val minAmount: BigDecimal? = null,
    val maxAmount: BigDecimal? = null,
    val tags: List<String>? = null,
    val merchantId: UUID? = null
)

data class BulkCreateTransactionRequest(
    val transactions: List<CreateTransactionRequest>
)

data class BulkTransactionResponse(
    val created: List<TransactionResponse>,
    val failed: List<BulkTransactionError>
)

data class BulkTransactionError(
    val index: Int,
    val error: String
)
