package com.fintogether.application.dto

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class CreateInstallmentGroupRequest(
    @field:NotNull(message = "Total amount is required")
    @field:DecimalMin(value = "0.01", message = "Total amount must be greater than 0")
    val totalAmount: BigDecimal,

    @field:NotNull(message = "Installment count is required")
    @field:Min(value = 2, message = "Installment count must be at least 2")
    @field:Max(value = 120, message = "Installment count must not exceed 120")
    val installmentCount: Int,

    val interestRate: BigDecimal = BigDecimal.ZERO,

    @field:NotNull(message = "Start date is required")
    val startDate: LocalDate,

    @field:Size(max = 500, message = "Description must not exceed 500 characters")
    val description: String? = null,

    val categoryId: UUID? = null,

    val accountId: UUID? = null,

    val cardId: UUID? = null,

    val visibility: String = "SHARED"
)

data class InstallmentGroupResponse(
    val id: UUID,
    val budgetId: UUID,
    val cardId: UUID?,
    val totalAmount: BigDecimal,
    val installmentCount: Int,
    val interestRate: BigDecimal,
    val startDate: LocalDate,
    val description: String?,
    val createdAt: LocalDateTime,
    val installmentAmount: BigDecimal,
    val totalWithInterest: BigDecimal,
    val status: String
)

data class InstallmentGroupProgressResponse(
    val id: UUID,
    val budgetId: UUID,
    val cardId: UUID?,
    val totalAmount: BigDecimal,
    val installmentCount: Int,
    val interestRate: BigDecimal,
    val startDate: LocalDate,
    val description: String?,
    val createdAt: LocalDateTime,
    val installmentAmount: BigDecimal,
    val totalWithInterest: BigDecimal,
    val status: String,
    val paidCount: Int,
    val paidAmount: BigDecimal,
    val remainingCount: Int,
    val remainingAmount: BigDecimal,
    val transactions: List<TransactionResponse>
)
