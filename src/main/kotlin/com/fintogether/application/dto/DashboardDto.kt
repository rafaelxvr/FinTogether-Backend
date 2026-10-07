package com.fintogether.application.dto

import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

/**
 * Dashboard Summary Response
 * Provides overview of financial status for a specific budget and month
 */
data class DashboardSummaryResponse(
    val budgetId: String,
    val period: String, // "2025-11"
    val totalIncome: BigDecimal,
    val totalExpense: BigDecimal,
    val netCashFlow: BigDecimal,
    val accountsBalance: BigDecimal,
    val categoryBreakdown: List<CategorySpendingDto>,
    val recentTransactions: List<TransactionSummaryDto>
)

/**
 * Category Spending DTO
 * Used for pie chart data and category breakdown
 */
data class CategorySpendingDto(
    val categoryId: String,
    val categoryName: String,
    val categoryType: String, // INCOME, EXPENSE, TRANSFER
    val amount: BigDecimal,
    val percentage: Double,
    val transactionCount: Int,
    val color: String? = null,
    val icon: String? = null,
    val isEssential: Boolean = false
)

/**
 * Transaction Summary DTO
 * Lightweight transaction info for recent transactions list
 */
data class TransactionSummaryDto(
    val id: String,
    val date: LocalDate,
    val description: String,
    val amount: BigDecimal,
    val type: String, // INCOME, EXPENSE, TRANSFER
    val categoryName: String?,
    val accountName: String?
)

/**
 * Cashflow Trend Response
 * Provides monthly income/expense data for trend charts
 */
data class CashflowTrendResponse(
    val budgetId: String,
    val months: List<MonthlyDataDto>
)

/**
 * Monthly Data DTO
 * Income/Expense data for a single month
 */
data class MonthlyDataDto(
    val period: String, // "2025-11"
    val yearMonth: YearMonth,
    val income: BigDecimal,
    val expense: BigDecimal,
    val net: BigDecimal,
    val transactionCount: Int
)

/**
 * Account Balance Summary
 * Current balance across all accounts grouped by type
 */
data class AccountBalanceSummaryDto(
    val totalBalance: BigDecimal,
    val byType: List<AccountTypeBalanceDto>
)

/**
 * Account Type Balance
 * Balance summary for a specific account type
 */
data class AccountTypeBalanceDto(
    val accountType: String, // CHECKING, SAVINGS, CASH, etc.
    val balance: BigDecimal,
    val accountCount: Int
)

/**
 * Dashboard Alerts Response
 * Warnings and upcoming events for user attention
 */
data class DashboardAlertsResponse(
    val lowBalanceAccounts: List<LowBalanceAlertDto>,
    val categoryLimitExceeded: List<CategoryLimitAlertDto>,
    val upcomingCreditCardDueDates: List<CreditCardDueAlertDto>
)

/**
 * Low Balance Alert
 * Accounts with balance below threshold
 */
data class LowBalanceAlertDto(
    val accountId: String,
    val accountName: String,
    val currentBalance: BigDecimal,
    val threshold: BigDecimal = BigDecimal.valueOf(100)
)

/**
 * Category Limit Alert
 * Categories that exceeded their budget limit
 */
data class CategoryLimitAlertDto(
    val categoryId: String,
    val categoryName: String,
    val limitAmount: BigDecimal,
    val currentSpending: BigDecimal,
    val percentage: Double
)

/**
 * Credit Card Due Alert
 * Upcoming credit card payment due dates
 */
data class CreditCardDueAlertDto(
    val cardId: String,
    val cardName: String,
    val dueDate: LocalDate,
    val statementAmount: BigDecimal,
    val daysUntilDue: Int,
    val isOverdue: Boolean
)

/**
 * Essential Breakdown Response
 * Shows spending split between essential (needs) and non-essential (wants) categories
 */
data class EssentialBreakdownResponse(
    val budgetId: String,
    val period: String,
    val essentialTotal: BigDecimal,
    val nonEssentialTotal: BigDecimal,
    val essentialPercentage: Double,
    val nonEssentialPercentage: Double,
    val essentialCategories: List<CategorySpendingDto>,
    val nonEssentialCategories: List<CategorySpendingDto>
)

/**
 * Top Categories Response
 * Top N spending/earning categories for the period
 */
data class TopCategoriesResponse(
    val budgetId: String,
    val period: String,
    val topExpenseCategories: List<CategorySpendingDto>,
    val topIncomeCategories: List<CategorySpendingDto>
)
