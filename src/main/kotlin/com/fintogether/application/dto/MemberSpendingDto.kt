package com.fintogether.application.dto

import java.math.BigDecimal

/**
 * Per-member spending data for shared budgets
 */
data class MemberSpendingDto(
    val userId: String,
    val userName: String,
    val totalExpense: BigDecimal,
    val totalIncome: BigDecimal,
    val transactionCount: Int,
    val percentage: Double
)

/**
 * Response wrapping member spending for a budget period
 */
data class MemberSpendingResponse(
    val budgetId: String,
    val period: String,
    val members: List<MemberSpendingDto>,
    val totalBudgetExpense: BigDecimal
)
