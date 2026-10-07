package com.fintogether.application.service

import com.fintogether.application.dto.*
import com.fintogether.application.mapper.TransactionMapper
import com.fintogether.domain.entity.*
import com.fintogether.domain.repository.*
import com.fintogether.infrastructure.security.UserPrincipal
import io.mockk.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

class TransactionServiceSearchTest {

    private lateinit var transactionRepository: TransactionRepository
    private lateinit var budgetRepository: BudgetRepository
    private lateinit var accountRepository: AccountRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var userRepository: UserRepository
    private lateinit var creditCardRepository: CreditCardRepository
    private lateinit var merchantRepository: MerchantRepository
    private lateinit var transactionMapper: TransactionMapper
    private lateinit var transactionSplitService: TransactionSplitService
    private lateinit var transactionService: TransactionService

    private lateinit var securityContext: SecurityContext
    private lateinit var authentication: Authentication

    private val userId = UUID.randomUUID()
    private val budgetId = UUID.randomUUID()
    private val categoryId = UUID.randomUUID()
    private val merchantId = UUID.randomUUID()

    private lateinit var owner: User
    private lateinit var budget: Budget
    private lateinit var expenseCategory: Category
    private val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "transactionDate"))

    @BeforeEach
    fun setUp() {
        transactionRepository = mockk()
        budgetRepository = mockk()
        accountRepository = mockk()
        categoryRepository = mockk()
        userRepository = mockk()
        creditCardRepository = mockk()
        merchantRepository = mockk()
        transactionMapper = mockk()
        transactionSplitService = mockk()

        transactionService = TransactionService(
            transactionRepository = transactionRepository,
            budgetRepository = budgetRepository,
            userRepository = userRepository,
            accountRepository = accountRepository,
            categoryRepository = categoryRepository,
            creditCardRepository = creditCardRepository,
            merchantRepository = merchantRepository,
            transactionMapper = transactionMapper,
            transactionSplitService = transactionSplitService
        )

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

        owner = createUser(userId, "user@example.com")
        budget = createBudget(budgetId, "My Budget", owner)
        budget.addMember(owner, MemberRole.OWNER)

        expenseCategory = Category(
            budget = budget,
            name = "Food",
            type = CategoryType.EXPENSE,
            id = categoryId
        )

        every { userRepository.findById(userId) } returns owner
        every { budgetRepository.findById(budgetId) } returns budget
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `searchTransactions by query text matches description`() {
        val transaction = createTransaction(description = "Pizza delivery")
        val response = mockk<TransactionResponse>()
        val page: Page<Transaction> = PageImpl(listOf(transaction), pageable, 1)

        every { transactionRepository.search(budgetId, "Pizza", null, null, null, null, null, null, null, pageable) } returns page
        every { transactionMapper.toResponse(transaction) } returns response

        val result = transactionService.searchTransactions(
            budgetId, "Pizza", null, null, null, null, null, null, null, pageable
        )

        assertEquals(1, result.content.size)
        verify { transactionRepository.search(budgetId, "Pizza", null, null, null, null, null, null, null, pageable) }
    }

    @Test
    fun `searchTransactions by query text matches notes`() {
        val transaction = createTransaction(notes = "Paid with cash")
        val response = mockk<TransactionResponse>()
        val page: Page<Transaction> = PageImpl(listOf(transaction), pageable, 1)

        every { transactionRepository.search(budgetId, "cash", null, null, null, null, null, null, null, pageable) } returns page
        every { transactionMapper.toResponse(transaction) } returns response

        val result = transactionService.searchTransactions(
            budgetId, "cash", null, null, null, null, null, null, null, pageable
        )

        assertEquals(1, result.content.size)
    }

    @Test
    fun `searchTransactions with type filter returns only matching type`() {
        val transaction = createTransaction(type = TransactionType.EXPENSE)
        val response = mockk<TransactionResponse>()
        val page: Page<Transaction> = PageImpl(listOf(transaction), pageable, 1)

        every { transactionRepository.search(budgetId, null, TransactionType.EXPENSE, null, null, null, null, null, null, pageable) } returns page
        every { transactionMapper.toResponse(transaction) } returns response

        val result = transactionService.searchTransactions(
            budgetId, null, TransactionType.EXPENSE, null, null, null, null, null, null, pageable
        )

        assertEquals(1, result.content.size)
    }

    @Test
    fun `searchTransactions with category filter narrows results`() {
        val transaction = createTransaction()
        val response = mockk<TransactionResponse>()
        val page: Page<Transaction> = PageImpl(listOf(transaction), pageable, 1)

        every { transactionRepository.search(budgetId, null, null, categoryId, null, null, null, null, null, pageable) } returns page
        every { transactionMapper.toResponse(transaction) } returns response

        val result = transactionService.searchTransactions(
            budgetId, null, null, categoryId, null, null, null, null, null, pageable
        )

        assertEquals(1, result.content.size)
    }

    @Test
    fun `searchTransactions with amount range returns transactions within range`() {
        val transaction = createTransaction(amount = BigDecimal("100.00"))
        val response = mockk<TransactionResponse>()
        val page: Page<Transaction> = PageImpl(listOf(transaction), pageable, 1)

        val minAmount = BigDecimal("50.00")
        val maxAmount = BigDecimal("200.00")

        every { transactionRepository.search(budgetId, null, null, null, null, minAmount, maxAmount, null, null, pageable) } returns page
        every { transactionMapper.toResponse(transaction) } returns response

        val result = transactionService.searchTransactions(
            budgetId, null, null, null, null, minAmount, maxAmount, null, null, pageable
        )

        assertEquals(1, result.content.size)
    }

    @Test
    fun `searchTransactions with date range returns transactions within range`() {
        val startDate = LocalDate.of(2026, 3, 1)
        val endDate = LocalDate.of(2026, 3, 15)
        val transaction = createTransaction()
        val response = mockk<TransactionResponse>()
        val page: Page<Transaction> = PageImpl(listOf(transaction), pageable, 1)

        every { transactionRepository.search(budgetId, null, null, null, null, null, null, startDate, endDate, pageable) } returns page
        every { transactionMapper.toResponse(transaction) } returns response

        val result = transactionService.searchTransactions(
            budgetId, null, null, null, null, null, null, startDate, endDate, pageable
        )

        assertEquals(1, result.content.size)
    }

    @Test
    fun `searchTransactions with combined filters all applied`() {
        val startDate = LocalDate.of(2026, 3, 1)
        val endDate = LocalDate.of(2026, 3, 15)
        val transaction = createTransaction(type = TransactionType.EXPENSE, description = "mercado")
        val response = mockk<TransactionResponse>()
        val page: Page<Transaction> = PageImpl(listOf(transaction), pageable, 1)

        every { transactionRepository.search(budgetId, "mercado", TransactionType.EXPENSE, categoryId, null, null, null, startDate, endDate, pageable) } returns page
        every { transactionMapper.toResponse(transaction) } returns response

        val result = transactionService.searchTransactions(
            budgetId, "mercado", TransactionType.EXPENSE, categoryId, null, null, null, startDate, endDate, pageable
        )

        assertEquals(1, result.content.size)
    }

    @Test
    fun `searchTransactions with empty query and no filters returns all transactions paginated`() {
        val transaction = createTransaction()
        val response = mockk<TransactionResponse>()
        val page: Page<Transaction> = PageImpl(listOf(transaction), pageable, 1)

        every { transactionRepository.search(budgetId, null, null, null, null, null, null, null, null, pageable) } returns page
        every { transactionMapper.toResponse(transaction) } returns response

        val result = transactionService.searchTransactions(
            budgetId, null, null, null, null, null, null, null, null, pageable
        )

        assertEquals(1, result.content.size)
    }

    @Test
    fun `searchTransactions with empty results returns empty page`() {
        val page: Page<Transaction> = PageImpl(emptyList(), pageable, 0)

        every { transactionRepository.search(budgetId, "xyznonexistent", null, null, null, null, null, null, null, pageable) } returns page

        val result = transactionService.searchTransactions(
            budgetId, "xyznonexistent", null, null, null, null, null, null, null, pageable
        )

        assertTrue(result.content.isEmpty())
        assertEquals(0, result.totalElements)
    }

    @Test
    fun `searchTransactions trims whitespace-only query to null`() {
        val transaction = createTransaction()
        val response = mockk<TransactionResponse>()
        val page: Page<Transaction> = PageImpl(listOf(transaction), pageable, 1)

        // When query is whitespace, it should be trimmed to null
        every { transactionRepository.search(budgetId, null, null, null, null, null, null, null, null, pageable) } returns page
        every { transactionMapper.toResponse(transaction) } returns response

        val result = transactionService.searchTransactions(
            budgetId, "   ", null, null, null, null, null, null, null, pageable
        )

        assertEquals(1, result.content.size)
        verify { transactionRepository.search(budgetId, null, null, null, null, null, null, null, null, pageable) }
    }

    // --- Helpers ---

    private fun createTransaction(
        type: TransactionType = TransactionType.EXPENSE,
        amount: BigDecimal = BigDecimal("50.00"),
        description: String = "Test transaction",
        notes: String? = null
    ): Transaction {
        return Transaction(
            budget = budget,
            user = owner,
            transactionDate = LocalDate.now(),
            amount = amount,
            type = type,
            category = expenseCategory,
            description = description,
            notes = notes,
            id = UUID.randomUUID()
        )
    }

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
