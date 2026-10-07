package com.fintogether.application.service

import com.fintogether.domain.entity.*
import com.fintogether.domain.repository.*
import io.mockk.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.YearMonth
import java.util.*

class DashboardServiceTest {

    private lateinit var budgetRepository: BudgetRepository
    private lateinit var accountRepository: AccountRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var userRepository: UserRepository
    private lateinit var dashboardService: DashboardService

    private val budgetId = UUID.randomUUID()
    private val userId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        budgetRepository = mockk()
        accountRepository = mockk()
        transactionRepository = mockk()
        userRepository = mockk()

        dashboardService = DashboardService(
            budgetRepository = budgetRepository,
            accountRepository = accountRepository,
            transactionRepository = transactionRepository,
            userRepository = userRepository
        )
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    // ==================== GET DASHBOARD SUMMARY TESTS ====================

    @Test
    fun `getDashboardSummary should calculate correct totals and breakdown`() {
        // Given
        val yearMonth = YearMonth.of(2025, 11)
        val startDate = yearMonth.atDay(1)
        val endDate = yearMonth.atEndOfMonth()

        val owner = createUser(userId, "owner@example.com")
        val budget = createBudget(budgetId, "My Budget", owner)
        budget.addMember(owner, MemberRole.OWNER)

        val account = Account(
            budget = budget,
            name = "Checking",
            type = AccountType.CHECKING,
            currentBalance = BigDecimal("5000.00"),
            active = true,
            id = UUID.randomUUID()
        )

        val foodCategory = Category(
            budget = budget,
            name = "Food",
            type = CategoryType.EXPENSE,
            id = UUID.randomUUID()
        )

        val salaryCategory = Category(
            budget = budget,
            name = "Salary",
            type = CategoryType.INCOME,
            id = UUID.randomUUID()
        )

        val incomeTransaction = Transaction(
            budget = budget,
            account = account,
            category = salaryCategory,
            type = TransactionType.INCOME,
            amount = BigDecimal("3000.00"),
            transactionDate = startDate,
            description = "Monthly Salary",
            id = UUID.randomUUID()
        )

        val expenseTransaction1 = Transaction(
            budget = budget,
            account = account,
            category = foodCategory,
            type = TransactionType.EXPENSE,
            amount = BigDecimal("500.00"),
            transactionDate = startDate.plusDays(5),
            description = "Groceries",
            id = UUID.randomUUID()
        )

        val expenseTransaction2 = Transaction(
            budget = budget,
            account = account,
            category = foodCategory,
            type = TransactionType.EXPENSE,
            amount = BigDecimal("300.00"),
            transactionDate = startDate.plusDays(10),
            description = "Restaurant",
            id = UUID.randomUUID()
        )

        val transactions = listOf(incomeTransaction, expenseTransaction1, expenseTransaction2)

        // Mock repository calls
        every { budgetRepository.findById(budgetId) } returns budget
        every { userRepository.findById(userId) } returns owner
        every { transactionRepository.findByBudgetIdAndDateBetween(budgetId, startDate, endDate) } returns transactions
        every { accountRepository.findByBudgetId(budgetId) } returns listOf(account)

        // When
        val response = dashboardService.getDashboardSummary(budgetId, yearMonth, userId)

        // Then
        assertNotNull(response)
        assertEquals(budgetId.toString(), response.budgetId)
        assertEquals(yearMonth.toString(), response.period)
        assertEquals(BigDecimal("3000.00"), response.totalIncome)
        assertEquals(BigDecimal("800.00"), response.totalExpense)
        assertEquals(BigDecimal("2200.00"), response.netCashFlow)
        assertEquals(BigDecimal("5000.00"), response.accountsBalance)
        assertEquals(1, response.categoryBreakdown.size)
        assertEquals(3, response.recentTransactions.size)

        verify(exactly = 1) { budgetRepository.findById(budgetId) }
        verify(exactly = 1) { transactionRepository.findByBudgetIdAndDateBetween(budgetId, startDate, endDate) }
        verify(exactly = 1) { accountRepository.findByBudgetId(budgetId) }
    }

    @Test
    fun `getDashboardSummary should return zero totals when no transactions exist`() {
        // Given
        val yearMonth = YearMonth.of(2025, 11)
        val startDate = yearMonth.atDay(1)
        val endDate = yearMonth.atEndOfMonth()

        val owner = createUser(userId, "owner@example.com")
        val budget = createBudget(budgetId, "My Budget", owner)
        budget.addMember(owner, MemberRole.OWNER)

        val account = Account(
            budget = budget,
            name = "Checking",
            type = AccountType.CHECKING,
            currentBalance = BigDecimal("1000.00"),
            active = true,
            id = UUID.randomUUID()
        )

        every { budgetRepository.findById(budgetId) } returns budget
        every { userRepository.findById(userId) } returns owner
        every { transactionRepository.findByBudgetIdAndDateBetween(budgetId, startDate, endDate) } returns emptyList()
        every { accountRepository.findByBudgetId(budgetId) } returns listOf(account)

        // When
        val response = dashboardService.getDashboardSummary(budgetId, yearMonth, userId)

        // Then
        assertNotNull(response)
        assertEquals(BigDecimal.ZERO, response.totalIncome)
        assertEquals(BigDecimal.ZERO, response.totalExpense)
        assertEquals(BigDecimal.ZERO, response.netCashFlow)
        assertEquals(BigDecimal("1000.00"), response.accountsBalance)
        assertTrue(response.categoryBreakdown.isEmpty())
        assertTrue(response.recentTransactions.isEmpty())

        verify(exactly = 1) { transactionRepository.findByBudgetIdAndDateBetween(budgetId, startDate, endDate) }
        verify(exactly = 1) { accountRepository.findByBudgetId(budgetId) }
    }

    @Test
    fun `getDashboardSummary should only include active accounts in balance calculation`() {
        // Given
        val yearMonth = YearMonth.of(2025, 11)
        val startDate = yearMonth.atDay(1)
        val endDate = yearMonth.atEndOfMonth()

        val owner = createUser(userId, "owner@example.com")
        val budget = createBudget(budgetId, "My Budget", owner)
        budget.addMember(owner, MemberRole.OWNER)

        val activeAccount = Account(
            budget = budget,
            name = "Active Checking",
            type = AccountType.CHECKING,
            currentBalance = BigDecimal("2000.00"),
            active = true,
            id = UUID.randomUUID()
        )

        val inactiveAccount = Account(
            budget = budget,
            name = "Inactive Savings",
            type = AccountType.SAVINGS,
            currentBalance = BigDecimal("5000.00"),
            active = false,
            id = UUID.randomUUID()
        )

        every { budgetRepository.findById(budgetId) } returns budget
        every { userRepository.findById(userId) } returns owner
        every { transactionRepository.findByBudgetIdAndDateBetween(budgetId, startDate, endDate) } returns emptyList()
        every { accountRepository.findByBudgetId(budgetId) } returns listOf(activeAccount, inactiveAccount)

        // When
        val response = dashboardService.getDashboardSummary(budgetId, yearMonth, userId)

        // Then
        assertNotNull(response)
        // Should only include active account balance
        assertEquals(BigDecimal("2000.00"), response.accountsBalance)

        verify(exactly = 1) { accountRepository.findByBudgetId(budgetId) }
    }

    // ==================== GET CASHFLOW TREND TESTS ====================

    @Test
    fun `getCashflowTrend should return monthly data for specified months`() {
        // Given
        val monthsCount = 3
        val currentMonth = YearMonth.now()

        val owner = createUser(userId, "owner@example.com")
        val budget = createBudget(budgetId, "My Budget", owner)
        budget.addMember(owner, MemberRole.OWNER)

        val account = Account(
            budget = budget,
            name = "Checking",
            type = AccountType.CHECKING,
            currentBalance = BigDecimal("5000.00"),
            active = true,
            id = UUID.randomUUID()
        )

        val category = Category(
            budget = budget,
            name = "Food",
            type = CategoryType.EXPENSE,
            id = UUID.randomUUID()
        )

        every { budgetRepository.findById(budgetId) } returns budget
        every { userRepository.findById(userId) } returns owner

        // Mock transactions for each month
        for (i in (monthsCount - 1) downTo 0) {
            val yearMonth = currentMonth.minusMonths(i.toLong())
            val startDate = yearMonth.atDay(1)
            val endDate = yearMonth.atEndOfMonth()

            val income = Transaction(
                budget = budget,
                account = account,
                category = null,
                type = TransactionType.INCOME,
                amount = BigDecimal("1000.00"),
                transactionDate = startDate,
                description = "Income",
                id = UUID.randomUUID()
            )

            val expense = Transaction(
                budget = budget,
                account = account,
                category = category,
                type = TransactionType.EXPENSE,
                amount = BigDecimal("600.00"),
                transactionDate = startDate.plusDays(5),
                description = "Expense",
                id = UUID.randomUUID()
            )

            every {
                transactionRepository.findByBudgetIdAndDateBetween(budgetId, startDate, endDate)
            } returns listOf(income, expense)
        }

        // When
        val response = dashboardService.getCashflowTrend(budgetId, monthsCount, userId)

        // Then
        assertNotNull(response)
        assertEquals(budgetId.toString(), response.budgetId)
        assertEquals(monthsCount, response.months.size)

        response.months.forEach { monthData ->
            assertEquals(BigDecimal("1000.00"), monthData.income)
            assertEquals(BigDecimal("600.00"), monthData.expense)
            assertEquals(BigDecimal("400.00"), monthData.net)
        }

        verify(exactly = 1) { budgetRepository.findById(budgetId) }
        verify(exactly = monthsCount) { transactionRepository.findByBudgetIdAndDateBetween(budgetId, any(), any()) }
    }

    @Test
    fun `getCashflowTrend should handle months with no transactions`() {
        // Given
        val monthsCount = 2
        val currentMonth = YearMonth.now()

        val owner = createUser(userId, "owner@example.com")
        val budget = createBudget(budgetId, "My Budget", owner)
        budget.addMember(owner, MemberRole.OWNER)

        every { budgetRepository.findById(budgetId) } returns budget
        every { userRepository.findById(userId) } returns owner

        // Mock empty transactions for all months
        for (i in (monthsCount - 1) downTo 0) {
            val yearMonth = currentMonth.minusMonths(i.toLong())
            val startDate = yearMonth.atDay(1)
            val endDate = yearMonth.atEndOfMonth()

            every {
                transactionRepository.findByBudgetIdAndDateBetween(budgetId, startDate, endDate)
            } returns emptyList()
        }

        // When
        val response = dashboardService.getCashflowTrend(budgetId, monthsCount, userId)

        // Then
        assertNotNull(response)
        assertEquals(monthsCount, response.months.size)

        response.months.forEach { monthData ->
            assertEquals(BigDecimal.ZERO, monthData.income)
            assertEquals(BigDecimal.ZERO, monthData.expense)
            assertEquals(BigDecimal.ZERO, monthData.net)
        }

        verify(exactly = 1) { budgetRepository.findById(budgetId) }
        verify(exactly = monthsCount) { transactionRepository.findByBudgetIdAndDateBetween(budgetId, any(), any()) }
    }

    // ==================== HELPER METHODS ====================

    private fun createUser(id: UUID, email: String): User {
        return User(
            email = email,
            passwordHash = "hash",
            name = "Test User",
            locale = "en-US",
            id = id
        )
    }

    private fun createBudget(id: UUID, name: String, owner: User): Budget {
        return Budget(
            name = name,
            owner = owner,
            type = BudgetType.PERSONAL,
            id = id
        )
    }
}
