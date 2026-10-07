package com.fintogether.application.dto

import java.math.BigDecimal

/**
 * Income source breakdown per category
 */
data class IncomeSourceDto(
    val categoryId: String,
    val categoryName: String,
    val amount: BigDecimal,
    val percentage: Double
)

/**
 * Per-member income data
 */
data class MemberIncomeDto(
    val userId: String,
    val userName: String,
    val totalIncome: BigDecimal,
    val percentage: Double,
    val transactionCount: Int,
    val topSources: List<IncomeSourceDto>
)

/**
 * Response wrapping income by member for a budget period
 */
data class IncomeByMemberResponse(
    val budgetId: String,
    val period: String,
    val totalBudgetIncome: BigDecimal,
    val members: List<MemberIncomeDto>
)
