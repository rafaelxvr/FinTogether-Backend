package com.fintogether.application.dto

import com.fintogether.domain.entity.CreditCard
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

/**
 * Credit Card Response DTO
 */
data class CreditCardResponse(
    val id: String,
    val budgetId: String,
    val name: String,
    val issuer: String?,
    val lastFour: String?,
    val currency: String,
    val statementCloseDay: Int,
    val dueDay: Int,
    val linkedAccountId: String?,
    val linkedAccountName: String?,
    val active: Boolean,
    val creditLimit: BigDecimal?,
    val createdAt: String
) {
    companion object {
        fun from(creditCard: CreditCard): CreditCardResponse {
            return CreditCardResponse(
                id = creditCard.id.toString(),
                budgetId = creditCard.budget.id.toString(),
                name = creditCard.name,
                issuer = creditCard.issuer,
                lastFour = creditCard.lastFour,
                currency = creditCard.currency,
                statementCloseDay = creditCard.statementCloseDay,
                dueDay = creditCard.dueDay,
                linkedAccountId = creditCard.linkedAccount?.id?.toString(),
                linkedAccountName = creditCard.linkedAccount?.name,
                active = creditCard.active,
                creditLimit = creditCard.creditLimit,
                createdAt = creditCard.createdAt.toString()
            )
        }
    }
}

/**
 * Create Credit Card Request DTO
 */
data class CreateCreditCardRequest(
    val budgetId: String,
    val name: String,
    val issuer: String? = null,
    val lastFour: String? = null,
    val currency: String = "BRL",
    val statementCloseDay: Int,
    val dueDay: Int,
    val linkedAccountId: String? = null,
    val creditLimit: BigDecimal? = null
) {
    init {
        require(name.isNotBlank()) { "Credit card name cannot be blank" }
        require(statementCloseDay in 1..31) { "Statement close day must be between 1 and 31" }
        require(dueDay in 1..31) { "Due day must be between 1 and 31" }
        lastFour?.let {
            require(it.length == 4 && it.all { char -> char.isDigit() }) {
                "Last four digits must be exactly 4 digits"
            }
        }
    }
}

/**
 * Update Credit Card Request DTO
 */
data class UpdateCreditCardRequest(
    val name: String? = null,
    val issuer: String? = null,
    val lastFour: String? = null,
    val currency: String? = null,
    val statementCloseDay: Int? = null,
    val dueDay: Int? = null,
    val linkedAccountId: String? = null,
    val creditLimit: BigDecimal? = null
) {
    init {
        name?.let { require(it.isNotBlank()) { "Credit card name cannot be blank" } }
        statementCloseDay?.let {
            require(it in 1..31) { "Statement close day must be between 1 and 31" }
        }
        dueDay?.let {
            require(it in 1..31) { "Due day must be between 1 and 31" }
        }
        lastFour?.let {
            require(it.length == 4 && it.all { char -> char.isDigit() }) {
                "Last four digits must be exactly 4 digits"
            }
        }
    }
}

/**
 * Link Account Request DTO
 */
data class LinkAccountRequest(
    val accountId: String
)

/**
 * Toggle Credit Card Status Request DTO
 */
data class ToggleCreditCardStatusRequest(
    val active: Boolean
)
