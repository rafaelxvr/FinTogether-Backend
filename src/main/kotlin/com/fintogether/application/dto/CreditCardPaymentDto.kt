package com.fintogether.application.dto

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

/**
 * Request to mark a credit card invoice as paid.
 */
data class MarkInvoicePaidRequest(
    @field:NotNull
    @field:DecimalMin("0.01")
    val paidAmount: BigDecimal,

    val paymentAccountId: UUID? = null,

    val notes: String? = null
)

/**
 * Response containing payment record details.
 */
data class CreditCardPaymentResponse(
    val id: String,
    val creditCardId: String,
    val referenceMonth: String,
    val paidAmount: BigDecimal,
    val paidAt: LocalDateTime,
    val paymentAccountId: String?,
    val paymentAccountName: String?,
    val notes: String?,
    val createdAt: LocalDateTime
)

/**
 * Response for payment suggestion (auto-detection of matching transfers).
 */
data class PaymentSuggestionResponse(
    val hasSuggestion: Boolean,
    val suggestedTransactionId: String? = null,
    val suggestedAmount: BigDecimal? = null,
    val suggestedAccountName: String? = null,
    val suggestedDate: LocalDate? = null,
    val message: String? = null
)
