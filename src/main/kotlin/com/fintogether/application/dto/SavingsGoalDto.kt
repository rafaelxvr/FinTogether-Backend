package com.fintogether.application.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class CreateSavingsGoalRequest(
    @field:NotBlank(message = "Savings goal name is required")
    @field:Size(min = 2, max = 255, message = "Name must be between 2 and 255 characters")
    val name: String,

    val description: String? = null,

    @field:NotNull(message = "Target amount is required")
    @field:Positive(message = "Target amount must be positive")
    val targetAmount: BigDecimal,

    val currency: String = "BRL",

    val deadline: LocalDate? = null,

    val accountId: UUID? = null,

    val icon: String? = null,

    val color: String? = null
)

data class UpdateSavingsGoalRequest(
    @field:NotBlank(message = "Savings goal name is required")
    @field:Size(min = 2, max = 255, message = "Name must be between 2 and 255 characters")
    val name: String,

    val description: String? = null,

    @field:Positive(message = "Target amount must be positive")
    val targetAmount: BigDecimal? = null,

    val deadline: LocalDate? = null,

    val icon: String? = null,

    val color: String? = null
)

data class ContributeRequest(
    @field:NotNull(message = "Amount is required")
    @field:Positive(message = "Amount must be positive")
    val amount: BigDecimal,

    val note: String? = null
)

data class WithdrawRequest(
    @field:NotNull(message = "Amount is required")
    @field:Positive(message = "Amount must be positive")
    val amount: BigDecimal,

    val note: String? = null
)

data class SavingsGoalResponse(
    val id: UUID,
    val budgetId: UUID,
    val accountId: UUID?,
    val name: String,
    val description: String?,
    val targetAmount: BigDecimal,
    val currentAmount: BigDecimal,
    val currency: String,
    val deadline: LocalDate?,
    val icon: String?,
    val color: String?,
    val active: Boolean,
    val progressPercentage: Double,
    val isCompleted: Boolean,
    val remainingAmount: BigDecimal,
    val daysRemaining: Long?,
    val completedAt: LocalDateTime?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime?
)

data class ContributionResponse(
    val id: UUID,
    val savingsGoalId: UUID,
    val amount: BigDecimal,
    val note: String?,
    val contributedAt: LocalDateTime
)
