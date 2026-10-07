package com.fintogether.application.dto

import com.fintogether.domain.entity.AccountType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

data class CreateAccountRequest(
    @field:NotBlank(message = "Account name is required")
    @field:Size(min = 2, max = 255, message = "Account name must be between 2 and 255 characters")
    val name: String,

    @field:NotNull(message = "Account type is required")
    val type: AccountType,

    val currency: String = "BRL",

    val openingBalance: BigDecimal = BigDecimal.ZERO
)

data class UpdateAccountRequest(
    @field:NotBlank(message = "Account name is required")
    @field:Size(min = 2, max = 255, message = "Account name must be between 2 and 255 characters")
    val name: String,

    val currency: String
)

data class AccountResponse(
    val id: UUID,
    val budgetId: UUID,
    val name: String,
    val type: AccountType,
    val currency: String,
    val openingBalance: BigDecimal,
    val currentBalance: BigDecimal,
    val active: Boolean,
    val createdAt: LocalDateTime
)

data class TransferRequest(
    @field:NotNull(message = "From account is required")
    val fromAccountId: UUID,

    @field:NotNull(message = "To account is required")
    val toAccountId: UUID,

    @field:NotNull(message = "Amount is required")
    val amount: BigDecimal,

    val description: String? = null
)
