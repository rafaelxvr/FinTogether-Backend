package com.fintogether.application.service

import com.fintogether.application.dto.*
import com.fintogether.application.mapper.TransactionMapper
import com.fintogether.domain.entity.*
import com.fintogether.domain.exception.AccessDeniedException
import com.fintogether.domain.exception.EntityNotFoundException
import com.fintogether.domain.exception.InvalidOperationException
import com.fintogether.domain.repository.*
import com.fintogether.infrastructure.security.UserPrincipal
import io.mockk.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

class TransactionServiceTest {

    private lateinit var transactionRepository: TransactionRepository
    private lateinit var budgetRepository: BudgetRepository
    private lateinit var accountRepository: AccountRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var userRepository: UserRepository
    private lateinit var creditCardRepository: CreditCardRepository
    private lateinit var transactionMapper: TransactionMapper
    private lateinit var transactionService: TransactionService

    private lateinit var securityContext: SecurityContext
    private lateinit var authentication: Authentication

    private val userId = UUID.randomUUID()
    private val budgetId = UUID.randomUUID()
    private val transactionId = UUID.randomUUID()
    private val accountId = UUID.randomUUID()
    private val categoryId = UUID.randomUUID()

    private lateinit var owner: User
    private lateinit var budget: Budget
    private lateinit var account: Account
    private lateinit var expenseCategory: Category

    @BeforeEach
    fun setUp() {
        transactionRepository = mockk()
        budgetRepository = mockk()
        accountRepository = mockk()
        categoryRepository = mockk()
        userRepository = mockk()
        creditCardRepository = mockk()
        transactionMapper = mockk()

        transactionService = TransactionService(
            transactionRepository = transactionRepository,
            budgetRepository = budgetRepository,
            userRepository = userRepository,
            accountRepository = accountRepository,
            categoryRepository = categoryRepository,
            creditCardRepository = creditCardRepository,
            transactionMapper = transactionMapper
        )

        // Mock SecurityContext
        securityContext = mockk()
        authentication = mockk()
        SecurityContextHolder.setContext(securityContext)

        every { securityContext.authentication } returns authentication
        every { authentication.principal } returns UserPrincipal(
            userId,
            "user@example.com",
            "password",
            listOf(SimpleGrantedAuthority("ROLE_USER"))
        )

        // Common test fixtures
        owner = createUser(userId, "user@example.com")
        budget = createBudget(budgetId, "My Budget", owner)
        budget.addMember(owner, MemberRole.OWNER)

        account = Account(
            budget = budget,
            name = "Checking",
            type = AccountType.CHECKING,
            currentBalance = BigDecimal("1000.00"),
            active = true,
            id = accountId
        )

        expenseCategory = Category(
            budget = budget,
            name = "Food",
            type = CategoryType.EXPENSE,
            id = categoryId
        )
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
        SecurityContextHolder.clearContext()
    }

    // ==================== CREATE TRANSACTION TESTS ====================

    @Test
    fun `createTransaction should create expense and update account balance`() {
        // Given
        val request = CreateTransactionRequest(
            transactionDate = LocalDate.now(),
            amount = BigDecimal("50.00"),
            type = TransactionType.EXPENSE,
            accountId = accountId,
            categoryId = categoryId,
            description = "Groceries"
        )

        val transaction = Transaction(
            budget = budget,
            user = owner,
            transactionDate = request.transactionDate,
            amount = request.amount,
            type = request.type,
            account = account,
            category = expenseCategory,
            description = request.description,
            id = transactionId
        )

        every { budgetRepository.findById(budgetId) } returns budget
        every { userRepository.findById(userId) } returns owner
        every { accountRepository.findById(accountId) } returns account
        every { categoryRepository.findById(categoryId) } returns expenseCategory
        every { accountRepository.save(any<Account>()) } returnsArgument 0
        every { transactionRepository.save(any<Transaction>()) } returns transaction
        every { transactionMapper.toResponse(transaction) } returns mockk()

        // When
        transactionService.createTransaction(budgetId, request)

        // Then
        verify(exactly = 1) { transactionRepository.save(any<Transaction>()) }
        verify(exactly = 1) { accountRepository.save(any<Account>()) }
    }

    @Test
    fun `createTransaction should throw EntityNotFoundException when budget not found`() {
        // Given
        val request = CreateTransactionRequest(
            transactionDate = LocalDate.now(),
            amount = BigDecimal("50.00"),
            type = TransactionType.EXPENSE,
            accountId = accountId,
            categoryId = categoryId,
            description = "Test"
        )

        every { budgetRepository.findById(budgetId) } returns null

        // When & Then
        assertThrows<EntityNotFoundException> {
            transactionService.createTransaction(budgetId, request)
        }

        verify(exactly = 0) { transactionRepository.save(any<Transaction>()) }
    }

    @Test
    fun `createTransaction should throw InvalidOperationException when account belongs to different budget`() {
        // Given
        val otherBudget = createBudget(UUID.randomUUID(), "Other", owner)
        val otherAccount = Account(
            budget = otherBudget,
            name = "Other",
            type = AccountType.CHECKING,
            currentBalance = BigDecimal.ZERO,
            active = true,
            id = accountId
        )

        val request = CreateTransactionRequest(
            transactionDate = LocalDate.now(),
            amount = BigDecimal("50.00"),
            type = TransactionType.EXPENSE,
            accountId = accountId,
            description = "Test"
        )

        every { budgetRepository.findById(budgetId) } returns budget
        every { userRepository.findById(userId) } returns owner
        every { accountRepository.findById(accountId) } returns otherAccount

        // When & Then
        assertThrows<InvalidOperationException> {
            transactionService.createTransaction(budgetId, request)
        }
    }

    @Test
    fun `createTransaction should throw InvalidOperationException when category type mismatches`() {
        // Given
        val incomeCategory = Category(
            budget = budget,
            name = "Salary",
            type = CategoryType.INCOME,
            id = categoryId
        )

        val request = CreateTransactionRequest(
            transactionDate = LocalDate.now(),
            amount = BigDecimal("50.00"),
            type = TransactionType.EXPENSE,
            accountId = accountId,
            categoryId = categoryId,
            description = "Test"
        )

        every { budgetRepository.findById(budgetId) } returns budget
        every { userRepository.findById(userId) } returns owner
        every { accountRepository.findById(accountId) } returns account
        every { categoryRepository.findById(categoryId) } returns incomeCategory

        // When & Then
        val exception = assertThrows<InvalidOperationException> {
            transactionService.createTransaction(budgetId, request)
        }

        assertEquals("Category type must match transaction type", exception.message)
    }

    // ==================== GET TRANSACTION TESTS ====================

    @Test
    fun `getTransactionById should return transaction when user has access`() {
        // Given
        val transaction = Transaction(
            budget = budget,
            user = owner,
            transactionDate = LocalDate.now(),
            amount = BigDecimal("50.00"),
            type = TransactionType.EXPENSE,
            account = account,
            description = "Test",
            id = transactionId
        )

        every { transactionRepository.findById(transactionId) } returns transaction
        every { budgetRepository.findById(budgetId) } returns budget
        every { userRepository.findById(userId) } returns owner
        every { transactionMapper.toResponse(transaction) } returns mockk()

        // When
        transactionService.getTransactionById(transactionId)

        // Then
        verify(exactly = 1) { transactionRepository.findById(transactionId) }
        verify(exactly = 1) { transactionMapper.toResponse(transaction) }
    }

    @Test
    fun `getTransactionById should throw EntityNotFoundException when transaction not found`() {
        // Given
        every { transactionRepository.findById(transactionId) } returns null

        // When & Then
        assertThrows<EntityNotFoundException> {
            transactionService.getTransactionById(transactionId)
        }

        verify(exactly = 1) { transactionRepository.findById(transactionId) }
    }

    @Test
    fun `getTransactionsByBudget should return transactions with date filter`() {
        // Given
        val startDate = LocalDate.of(2025, 1, 1)
        val endDate = LocalDate.of(2025, 1, 31)
        val transactions = listOf(
            Transaction(budget = budget, user = owner, transactionDate = startDate,
                amount = BigDecimal("100.00"), type = TransactionType.INCOME,
                description = "Salary", id = UUID.randomUUID())
        )

        every { budgetRepository.findById(budgetId) } returns budget
        every { userRepository.findById(userId) } returns owner
        every { transactionRepository.findByBudgetIdAndDateBetween(budgetId, startDate, endDate) } returns transactions
        every { transactionMapper.toResponse(any()) } returns mockk()

        // When
        val result = transactionService.getTransactionsByBudget(budgetId, startDate, endDate)

        // Then
        assertEquals(1, result.size)
        verify(exactly = 1) { transactionRepository.findByBudgetIdAndDateBetween(budgetId, startDate, endDate) }
    }

    @Test
    fun `getTransactionsByBudget should return all transactions when no date filter`() {
        // Given
        every { budgetRepository.findById(budgetId) } returns budget
        every { userRepository.findById(userId) } returns owner
        every { transactionRepository.findByBudgetId(budgetId) } returns emptyList()

        // When
        val result = transactionService.getTransactionsByBudget(budgetId, null, null)

        // Then
        assertTrue(result.isEmpty())
        verify(exactly = 1) { transactionRepository.findByBudgetId(budgetId) }
    }

    // ==================== DELETE TRANSACTION TESTS ====================

    @Test
    fun `deleteTransaction should reverse account balance for expense`() {
        // Given
        val transaction = Transaction(
            budget = budget,
            user = owner,
            transactionDate = LocalDate.now(),
            amount = BigDecimal("50.00"),
            type = TransactionType.EXPENSE,
            account = account,
            description = "Test",
            id = transactionId
        )

        every { transactionRepository.findById(transactionId) } returns transaction
        every { budgetRepository.findById(budgetId) } returns budget
        every { userRepository.findById(userId) } returns owner
        every { accountRepository.save(any<Account>()) } returnsArgument 0
        every { transactionRepository.delete(transaction) } just Runs

        // When
        transactionService.deleteTransaction(transactionId)

        // Then
        verify(exactly = 1) { accountRepository.save(any<Account>()) }
        verify(exactly = 1) { transactionRepository.delete(transaction) }
    }

    // ==================== BULK CREATE TESTS ====================

    @Test
    fun `bulkCreateTransactions should create multiple transactions and report failures`() {
        // Given
        val validRequest = CreateTransactionRequest(
            transactionDate = LocalDate.now(),
            amount = BigDecimal("50.00"),
            type = TransactionType.EXPENSE,
            accountId = accountId,
            description = "Valid"
        )

        val bulkRequest = BulkCreateTransactionRequest(
            transactions = listOf(validRequest, validRequest)
        )

        val transaction = Transaction(
            budget = budget,
            user = owner,
            transactionDate = LocalDate.now(),
            amount = BigDecimal("50.00"),
            type = TransactionType.EXPENSE,
            account = account,
            description = "Valid",
            id = UUID.randomUUID()
        )

        every { budgetRepository.findById(budgetId) } returns budget
        every { userRepository.findById(userId) } returns owner
        every { accountRepository.findById(accountId) } returns account
        every { accountRepository.save(any<Account>()) } returnsArgument 0
        every { transactionRepository.save(any<Transaction>()) } returns transaction
        every { transactionMapper.toResponse(any()) } returns mockk()

        // When
        val result = transactionService.bulkCreateTransactions(budgetId, bulkRequest)

        // Then
        assertEquals(2, result.created.size)
        assertEquals(0, result.failed.size)
    }

    // ==================== ACCESS DENIED TESTS ====================

    @Test
    fun `createTransaction should throw AccessDeniedException when user has no access`() {
        // Given
        val otherOwner = createUser(UUID.randomUUID(), "other@example.com")
        val otherBudget = createBudget(budgetId, "Other Budget", otherOwner)

        val request = CreateTransactionRequest(
            transactionDate = LocalDate.now(),
            amount = BigDecimal("50.00"),
            type = TransactionType.EXPENSE,
            description = "Test"
        )

        every { budgetRepository.findById(budgetId) } returns otherBudget
        every { userRepository.findById(userId) } returns owner

        // When & Then
        assertThrows<AccessDeniedException> {
            transactionService.createTransaction(budgetId, request)
        }
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
