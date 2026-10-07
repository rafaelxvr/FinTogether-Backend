package com.fintogether.application.service

import com.fintogether.application.dto.*
import com.fintogether.domain.entity.TransactionType
import com.fintogether.domain.exception.AccessDeniedException
import com.fintogether.domain.exception.EntityNotFoundException
import com.fintogether.domain.repository.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.*

@Service
@Transactional(readOnly = true)
class DashboardService(
    private val budgetRepository: BudgetRepository,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val creditCardRepository: CreditCardRepository,
    private val creditCardInvoiceService: CreditCardInvoiceService
) {

    private val logger = LoggerFactory.getLogger(DashboardService::class.java)

    /**
     * Get comprehensive dashboard summary for a budget and month
     */
    fun getDashboardSummary(budgetId: UUID, yearMonth: YearMonth, userId: UUID): DashboardSummaryResponse {
        logger.debug("Getting dashboard summary for budget: $budgetId, month: $yearMonth")
        // Validate budget access
        validateBudgetAccess(budgetId, userId)

        val startDate = yearMonth.atDay(1)
        val endDate = yearMonth.atEndOfMonth()

        // Get all transactions for the month
        val transactions = transactionRepository.findByBudgetIdAndDateBetween(budgetId, startDate, endDate)

        // Calculate totals
        val totalIncome = transactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amount }

        val totalExpense = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }

        val netCashFlow = totalIncome.subtract(totalExpense)

        // Get account balances
        val accounts = accountRepository.findByBudgetId(budgetId)
        val accountsBalance = accounts
            .filter { it.active }
            .sumOf { it.currentBalance }

        // Calculate category breakdown
        val categoryBreakdown = calculateCategoryBreakdown(transactions, totalExpense)

        // Get recent transactions (last 10)
        val recentTransactions = transactions
            .sortedByDescending { it.transactionDate }
            .take(10)
            .map { transaction ->
                TransactionSummaryDto(
                    id = transaction.id.toString(),
                    date = transaction.transactionDate,
                    description = transaction.description ?: "No description",
                    amount = transaction.amount,
                    type = transaction.type.name,
                    categoryName = transaction.category?.name,
                    accountName = transaction.account?.name
                )
            }

        return DashboardSummaryResponse(
            budgetId = budgetId.toString(),
            period = yearMonth.toString(),
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netCashFlow = netCashFlow,
            accountsBalance = accountsBalance,
            categoryBreakdown = categoryBreakdown,
            recentTransactions = recentTransactions
        )
    }

    /**
     * Get cashflow trend data for multiple months
     */
    fun getCashflowTrend(budgetId: UUID, monthsCount: Int, userId: UUID): CashflowTrendResponse {
        logger.debug("Getting cashflow trend for budget: $budgetId, months: $monthsCount")
        validateBudgetAccess(budgetId, userId)

        val currentMonth = YearMonth.now()
        val months = mutableListOf<MonthlyDataDto>()

        for (i in (monthsCount - 1) downTo 0) {
            val yearMonth = currentMonth.minusMonths(i.toLong())
            val startDate = yearMonth.atDay(1)
            val endDate = yearMonth.atEndOfMonth()

            val transactions = transactionRepository.findByBudgetIdAndDateBetween(budgetId, startDate, endDate)

            val income = transactions
                .filter { it.type == TransactionType.INCOME }
                .sumOf { it.amount }

            val expense = transactions
                .filter { it.type == TransactionType.EXPENSE }
                .sumOf { it.amount }

            val net = income.subtract(expense)

            months.add(
                MonthlyDataDto(
                    period = yearMonth.toString(),
                    yearMonth = yearMonth,
                    income = income,
                    expense = expense,
                    net = net,
                    transactionCount = transactions.size
                )
            )
        }

        return CashflowTrendResponse(
            budgetId = budgetId.toString(),
            months = months
        )
    }

    /**
     * Get category breakdown for a specific period
     */
    fun getCategoryBreakdown(budgetId: UUID, yearMonth: YearMonth, userId: UUID): TopCategoriesResponse {
        logger.debug("Getting category breakdown for budget: $budgetId, month: $yearMonth")
        validateBudgetAccess(budgetId, userId)

        val startDate = yearMonth.atDay(1)
        val endDate = yearMonth.atEndOfMonth()
        val transactions = transactionRepository.findByBudgetIdAndDateBetween(budgetId, startDate, endDate)

        // Group by category and type
        val expenseTransactions = transactions.filter { it.type == TransactionType.EXPENSE }
        val incomeTransactions = transactions.filter { it.type == TransactionType.INCOME }

        val totalExpense = expenseTransactions.sumOf { it.amount }
        val totalIncome = incomeTransactions.sumOf { it.amount }

        val topExpenseCategories = calculateCategoryBreakdown(expenseTransactions, totalExpense)
            .sortedByDescending { it.amount }
            .take(5)

        val topIncomeCategories = calculateCategoryBreakdown(incomeTransactions, totalIncome)
            .sortedByDescending { it.amount }
            .take(5)

        return TopCategoriesResponse(
            budgetId = budgetId.toString(),
            period = yearMonth.toString(),
            topExpenseCategories = topExpenseCategories,
            topIncomeCategories = topIncomeCategories
        )
    }

    /**
     * Get account balance summary grouped by type
     */
    fun getAccountBalanceSummary(budgetId: UUID, userId: UUID): AccountBalanceSummaryDto {
        logger.debug("Getting account balance summary for budget: $budgetId")
        validateBudgetAccess(budgetId, userId)

        val accounts = accountRepository.findByBudgetId(budgetId)
            .filter { it.active }

        val totalBalance = accounts.sumOf { it.currentBalance }

        val byType = accounts
            .groupBy { it.type }
            .map { (type, accountList) ->
                AccountTypeBalanceDto(
                    accountType = type.name,
                    balance = accountList.sumOf { it.currentBalance },
                    accountCount = accountList.size
                )
            }
            .sortedByDescending { it.balance }

        return AccountBalanceSummaryDto(
            totalBalance = totalBalance,
            byType = byType
        )
    }

    /**
     * Get dashboard alerts and warnings
     */
    fun getDashboardAlerts(budgetId: UUID, userId: UUID): DashboardAlertsResponse {
        logger.debug("Getting dashboard alerts for budget: $budgetId")
        validateBudgetAccess(budgetId, userId)

        // Low balance accounts (< 100.00)
        val lowBalanceThreshold = BigDecimal.valueOf(100)
        val lowBalanceAccounts = accountRepository.findByBudgetId(budgetId)
            .filter { it.active && it.currentBalance < lowBalanceThreshold }
            .map { account ->
                LowBalanceAlertDto(
                    accountId = account.id.toString(),
                    accountName = account.name,
                    currentBalance = account.currentBalance,
                    threshold = lowBalanceThreshold
                )
            }

        // Categories exceeding limits
        val currentMonth = YearMonth.now()
        val startDate = currentMonth.atDay(1)
        val endDate = currentMonth.atEndOfMonth()
        val transactions = transactionRepository.findByBudgetIdAndDateBetween(budgetId, startDate, endDate)

        val categoryLimitExceeded = transactions
            .filter { it.type == TransactionType.EXPENSE && it.category?.limitAmount != null }
            .groupBy { it.category!! }
            .mapNotNull { (category, transactionList) ->
                val currentSpending = transactionList.sumOf { it.amount }
                val limit = category.limitAmount!!

                if (currentSpending > limit) {
                    val percentage = currentSpending.divide(limit, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .toDouble()

                    CategoryLimitAlertDto(
                        categoryId = category.id.toString(),
                        categoryName = category.name,
                        limitAmount = limit,
                        currentSpending = currentSpending,
                        percentage = percentage
                    )
                } else null
            }

        // Credit card due date alerts
        val upcomingCreditCardDueDates = computeCreditCardAlerts(budgetId, userId)

        return DashboardAlertsResponse(
            lowBalanceAccounts = lowBalanceAccounts,
            categoryLimitExceeded = categoryLimitExceeded,
            upcomingCreditCardDueDates = upcomingCreditCardDueDates
        )
    }

    /**
     * Compute credit card alerts for cards due within 15 days or overdue.
     */
    private fun computeCreditCardAlerts(budgetId: UUID, userId: UUID): List<CreditCardDueAlertDto> {
        val budget = budgetRepository.findById(budgetId) ?: return emptyList()
        val activeCards = creditCardRepository.findByBudgetAndActive(budget, true)
        val today = LocalDate.now()
        val currentMonth = YearMonth.now()

        return activeCards.mapNotNull { card ->
            try {
                val invoice = creditCardInvoiceService.getInvoice(card.id, currentMonth, userId)
                val dueDate = invoice.dueDate
                val daysUntilDue = ChronoUnit.DAYS.between(today, dueDate).toInt()
                val isOverdue = daysUntilDue < 0

                if (isOverdue || daysUntilDue <= 15) {
                    CreditCardDueAlertDto(
                        cardId = card.id.toString(),
                        cardName = card.name,
                        dueDate = dueDate,
                        statementAmount = invoice.totalAmount,
                        daysUntilDue = daysUntilDue,
                        isOverdue = isOverdue
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                logger.warn("Failed to compute invoice alert for card ${card.id}: ${e.message}")
                null
            }
        }
    }

    /**
     * Calculate category breakdown from transactions
     */
    private fun calculateCategoryBreakdown(
        transactions: List<com.fintogether.domain.entity.Transaction>,
        total: BigDecimal
    ): List<CategorySpendingDto> {
        return transactions
            .filter { it.category != null }
            .groupBy { it.category!! }
            .map { (category, transactionList) ->
                val amount = transactionList.sumOf { it.amount }
                val percentage = if (total > BigDecimal.ZERO) {
                    amount.divide(total, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .toDouble()
                } else 0.0

                CategorySpendingDto(
                    categoryId = category.id.toString(),
                    categoryName = category.name,
                    categoryType = category.type.name,
                    amount = amount,
                    percentage = percentage,
                    transactionCount = transactionList.size,
                    color = category.color,
                    icon = category.icon,
                    isEssential = category.isEssential
                )
            }
            .sortedByDescending { it.amount }
    }

    /**
     * Get essential vs non-essential spending breakdown for a budget and month
     */
    fun getEssentialBreakdown(budgetId: UUID, yearMonth: YearMonth, userId: UUID): EssentialBreakdownResponse {
        logger.debug("Getting essential breakdown for budget: $budgetId, month: $yearMonth")
        validateBudgetAccess(budgetId, userId)

        val startDate = yearMonth.atDay(1)
        val endDate = yearMonth.atEndOfMonth()

        val transactions = transactionRepository.findByBudgetIdAndDateBetween(budgetId, startDate, endDate)

        val expenseTransactions = transactions.filter { it.type == TransactionType.EXPENSE }
        val totalExpense = expenseTransactions.sumOf { it.amount }

        val (essentialTransactions, nonEssentialTransactions) = expenseTransactions
            .partition { it.category?.isEssential == true }

        val essentialTotal = essentialTransactions.sumOf { it.amount }
        val nonEssentialTotal = nonEssentialTransactions.sumOf { it.amount }

        val essentialPercentage = if (totalExpense > BigDecimal.ZERO) {
            essentialTotal.divide(totalExpense, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .toDouble()
        } else 0.0

        val nonEssentialPercentage = if (totalExpense > BigDecimal.ZERO) {
            nonEssentialTotal.divide(totalExpense, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .toDouble()
        } else 0.0

        val essentialCategories = calculateCategoryBreakdown(essentialTransactions, essentialTotal)
        val nonEssentialCategories = calculateCategoryBreakdown(nonEssentialTransactions, nonEssentialTotal)

        return EssentialBreakdownResponse(
            budgetId = budgetId.toString(),
            period = yearMonth.toString(),
            essentialTotal = essentialTotal,
            nonEssentialTotal = nonEssentialTotal,
            essentialPercentage = essentialPercentage,
            nonEssentialPercentage = nonEssentialPercentage,
            essentialCategories = essentialCategories,
            nonEssentialCategories = nonEssentialCategories
        )
    }

    /**
     * Get per-member spending breakdown for shared budgets
     */
    fun getMemberSpending(budgetId: UUID, yearMonth: YearMonth, userId: UUID): MemberSpendingResponse {
        logger.debug("Getting member spending for budget: $budgetId, month: $yearMonth")
        validateBudgetAccess(budgetId, userId)

        val startDate = yearMonth.atDay(1)
        val endDate = yearMonth.atEndOfMonth()
        val transactions = transactionRepository.findByBudgetIdAndDateBetween(budgetId, startDate, endDate)

        val expenseTransactions = transactions.filter { it.type == TransactionType.EXPENSE }
        val totalBudgetExpense = expenseTransactions.sumOf { it.amount }

        val memberSpending = expenseTransactions
            .groupBy { it.user }
            .map { (user, txns) ->
                val totalExpense = txns.sumOf { it.amount }
                val incomeTransactions = transactions.filter { it.type == TransactionType.INCOME && it.user == user }
                val totalIncome = incomeTransactions.sumOf { it.amount }
                val percentage = if (totalBudgetExpense > BigDecimal.ZERO) {
                    totalExpense.divide(totalBudgetExpense, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .toDouble()
                } else 0.0

                MemberSpendingDto(
                    userId = user?.id?.toString() ?: "unattributed",
                    userName = user?.name ?: "Unattributed",
                    totalExpense = totalExpense,
                    totalIncome = totalIncome,
                    transactionCount = txns.size,
                    percentage = percentage
                )
            }
            .sortedByDescending { it.totalExpense }

        return MemberSpendingResponse(
            budgetId = budgetId.toString(),
            period = yearMonth.toString(),
            members = memberSpending,
            totalBudgetExpense = totalBudgetExpense
        )
    }

    /**
     * Get expense forecast based on current month spending pace
     */
    fun getExpenseForecast(budgetId: UUID, userId: UUID): ExpenseForecastDto {
        logger.debug("Getting expense forecast for budget: $budgetId")
        validateBudgetAccess(budgetId, userId)

        val currentMonth = YearMonth.now()
        val today = LocalDate.now()
        val startDate = currentMonth.atDay(1)
        val endDate = currentMonth.atEndOfMonth()
        val daysInMonth = currentMonth.lengthOfMonth()
        val daysElapsed = today.dayOfMonth.coerceAtLeast(1)
        val daysRemaining = daysInMonth - daysElapsed

        val transactions = transactionRepository.findByBudgetIdAndDateBetween(budgetId, startDate, today)
        val currentMonthExpense = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }

        val dailyAverage = if (daysElapsed > 0) {
            currentMonthExpense.divide(BigDecimal.valueOf(daysElapsed.toLong()), 2, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO

        val projectedMonthEndExpense = dailyAverage.multiply(BigDecimal.valueOf(daysInMonth.toLong()))

        // Get last month's total
        val lastMonth = currentMonth.minusMonths(1)
        val lastMonthStart = lastMonth.atDay(1)
        val lastMonthEnd = lastMonth.atEndOfMonth()
        val lastMonthTransactions = transactionRepository.findByBudgetIdAndDateBetween(budgetId, lastMonthStart, lastMonthEnd)
        val lastMonthTotal = lastMonthTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }

        val projectedVsLastMonth = if (lastMonthTotal > BigDecimal.ZERO) {
            projectedMonthEndExpense.subtract(lastMonthTotal)
                .divide(lastMonthTotal, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .toDouble()
        } else 0.0

        return ExpenseForecastDto(
            currentMonthExpense = currentMonthExpense,
            projectedMonthEndExpense = projectedMonthEndExpense,
            dailyAverage = dailyAverage,
            daysElapsed = daysElapsed,
            daysRemaining = daysRemaining,
            lastMonthTotal = lastMonthTotal,
            projectedVsLastMonth = projectedVsLastMonth
        )
    }

    /**
     * Get income grouped by member for shared budgets
     */
    fun getIncomeByMember(budgetId: UUID, yearMonth: YearMonth, userId: UUID): IncomeByMemberResponse {
        logger.debug("Getting income by member for budget: $budgetId, month: $yearMonth")
        validateBudgetAccess(budgetId, userId)

        val startDate = yearMonth.atDay(1)
        val endDate = yearMonth.atEndOfMonth()
        val transactions = transactionRepository.findByBudgetIdAndDateBetween(budgetId, startDate, endDate)
        val incomeTransactions = transactions.filter { it.type == TransactionType.INCOME }
        val totalBudgetIncome = incomeTransactions.sumOf { it.amount }

        val members = incomeTransactions
            .groupBy { it.user }
            .map { (user, txns) ->
                val totalIncome = txns.sumOf { it.amount }
                val percentage = if (totalBudgetIncome > BigDecimal.ZERO) {
                    totalIncome.divide(totalBudgetIncome, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .toDouble()
                } else 0.0

                val topSources = txns
                    .filter { it.category != null }
                    .groupBy { it.category!! }
                    .map { (category, catTxns) ->
                        val amount = catTxns.sumOf { it.amount }
                        val catPercentage = if (totalIncome > BigDecimal.ZERO) {
                            amount.divide(totalIncome, 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100))
                                .toDouble()
                        } else 0.0
                        IncomeSourceDto(
                            categoryId = category.id.toString(),
                            categoryName = category.name,
                            amount = amount,
                            percentage = catPercentage
                        )
                    }
                    .sortedByDescending { it.amount }
                    .take(5)

                MemberIncomeDto(
                    userId = user?.id?.toString() ?: "unattributed",
                    userName = user?.name ?: "Unattributed",
                    totalIncome = totalIncome,
                    percentage = percentage,
                    transactionCount = txns.size,
                    topSources = topSources
                )
            }
            .sortedByDescending { it.totalIncome }

        return IncomeByMemberResponse(
            budgetId = budgetId.toString(),
            period = yearMonth.toString(),
            totalBudgetIncome = totalBudgetIncome,
            members = members
        )
    }

    /**
     * Validate that user has access to budget.
     * Compares IDs directly instead of loading the User entity,
     * since the userId is already validated by the JWT filter.
     */
    private fun validateBudgetAccess(budgetId: UUID, userId: UUID) {
        val budget = budgetRepository.findById(budgetId)
            ?: throw EntityNotFoundException("Budget", budgetId)

        val isOwner = budget.owner.id == userId
        val isMember = budget.members.any { it.user.id == userId }

        if (!isOwner && !isMember) {
            logger.warn("Access denied for user $userId to budget $budgetId")
            throw AccessDeniedException("User does not have access to this budget", "Budget", budgetId)
        }
    }
}
