package com.fintogether.application.dto

import java.math.BigDecimal
import java.time.LocalDate

/**
 * Response for a single card's monthly cost within a billing cycle
 */
data class CardMonthlyCostResponse(
    val cardId: String,
    val cardName: String,
    val billingCycleStart: LocalDate,
    val billingCycleEnd: LocalDate,
    val totalCost: BigDecimal,
    val transactionCount: Int
)

/**
 * Response wrapping multiple months of cost data for a single card (comparison)
 */
data class CardCostComparisonResponse(
    val cardId: String,
    val cardName: String,
    val months: List<CardMonthlyCostResponse>
)

/**
 * Response summarizing all cards' costs for a budget in a given month
 */
data class AllCardsCostSummaryResponse(
    val budgetId: String,
    val period: String,
    val cards: List<CardMonthlyCostResponse>,
    val grandTotal: BigDecimal
)
