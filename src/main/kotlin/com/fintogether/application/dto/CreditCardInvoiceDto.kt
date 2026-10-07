package com.fintogether.application.dto

import java.math.BigDecimal
import java.time.LocalDate

/**
 * Invoice Status
 * Represents the lifecycle state of a credit card invoice
 */
enum class InvoiceStatus {
    OPEN,
    CLOSED,
    OVERDUE,
    PAID
}

/**
 * Credit Card Invoice Response
 * Full invoice details for a specific card and reference month
 */
data class CreditCardInvoiceResponse(
    val cardId: String,
    val cardName: String,
    val referenceMonth: String,
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val dueDate: LocalDate,
    val totalAmount: BigDecimal,
    val transactionCount: Int,
    val transactions: List<InvoiceTransactionDto>,
    val status: InvoiceStatus,
    val creditLimit: BigDecimal? = null,
    val availableCredit: BigDecimal? = null
)

/**
 * Invoice Transaction DTO
 * Represents a single transaction within an invoice
 */
data class InvoiceTransactionDto(
    val id: String,
    val transactionDate: LocalDate,
    val description: String? = null,
    val categoryName: String? = null,
    val categoryId: String? = null,
    val amount: BigDecimal,
    val installmentInfo: String? = null
)

/**
 * Invoice Summary Response
 * Lightweight list of invoices for a specific card
 */
data class InvoiceSummaryResponse(
    val cardId: String,
    val cardName: String,
    val invoices: List<InvoiceSummaryDto>
)

/**
 * Invoice Summary DTO
 * Lightweight invoice info without transaction details
 */
data class InvoiceSummaryDto(
    val referenceMonth: String,
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val dueDate: LocalDate,
    val totalAmount: BigDecimal,
    val transactionCount: Int,
    val status: InvoiceStatus
)
