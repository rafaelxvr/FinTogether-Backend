package com.fintogether.application.dto

import java.math.BigDecimal

/**
 * Expense forecast data based on current month spending pace
 */
data class ExpenseForecastDto(
    val currentMonthExpense: BigDecimal,
    val projectedMonthEndExpense: BigDecimal,
    val dailyAverage: BigDecimal,
    val daysElapsed: Int,
    val daysRemaining: Int,
    val lastMonthTotal: BigDecimal,
    val projectedVsLastMonth: Double
)
