package com.fintogether.application.dto

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class CreateRecurringTemplateRequest(
    @field:NotNull(message = "Amount is required")
    val amount: BigDecimal,

    @field:NotNull(message = "Transaction type is required")
    @field:Size(max = 20, message = "Type must not exceed 20 characters")
    val type: String,

    val accountId: UUID? = null,

    val categoryId: UUID? = null,

    val cardId: UUID? = null,

    @field:Size(max = 500, message = "Description must not exceed 500 characters")
    val description: String? = null,

    @field:Size(max = 1000, message = "Notes must not exceed 1000 characters")
    val notes: String? = null,

    val tags: String? = null,

    val visibility: String = "SHARED",

    val isFixedExpense: Boolean = false,

    @field:NotNull(message = "Recurrence rule is required")
    val recurrenceRule: Map<String, Any>,

    @field:NotNull(message = "Next due date is required")
    val nextDueDate: LocalDate
)

data class UpdateRecurringTemplateRequest(
    val amount: BigDecimal? = null,

    @field:Size(max = 20, message = "Type must not exceed 20 characters")
    val type: String? = null,

    val accountId: UUID? = null,

    val categoryId: UUID? = null,

    val cardId: UUID? = null,

    @field:Size(max = 500, message = "Description must not exceed 500 characters")
    val description: String? = null,

    @field:Size(max = 1000, message = "Notes must not exceed 1000 characters")
    val notes: String? = null,

    val tags: String? = null,

    val visibility: String? = null,

    val isFixedExpense: Boolean? = null,

    val recurrenceRule: Map<String, Any>? = null,

    val nextDueDate: LocalDate? = null
)

data class RecurringTemplateResponse(
    val id: UUID,
    val budgetId: UUID,
    val userId: UUID,
    val amount: BigDecimal,
    val type: String,
    val accountId: UUID?,
    val categoryId: UUID?,
    val cardId: UUID?,
    val description: String?,
    val notes: String?,
    val tags: String?,
    val visibility: String,
    val isFixedExpense: Boolean,
    val recurrenceRule: Map<String, Any>,
    val lastGeneratedDate: LocalDate?,
    val nextDueDate: LocalDate,
    val isActive: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class FixedExpenseSummaryResponse(
    val budgetId: UUID,
    val totalFixedExpenses: BigDecimal,
    val activeTemplateCount: Int,
    val templates: List<RecurringTemplateResponse>
)
