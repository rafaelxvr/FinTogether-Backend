package com.fintogether.application.service

import com.fintogether.application.dto.CreateAccountRequest
import com.fintogether.application.dto.TransferRequest
import com.fintogether.application.dto.UpdateAccountRequest
import com.fintogether.application.mapper.AccountMapper
import com.fintogether.domain.entity.*
import com.fintogether.domain.exception.AccessDeniedException
import com.fintogether.domain.exception.EntityNotFoundException
import com.fintogether.domain.exception.InvalidOperationException
import com.fintogether.domain.repository.AccountRepository
import com.fintogether.domain.repository.BudgetRepository
import com.fintogether.domain.repository.UserRepository
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
import java.util.*

class AccountServiceTest {

    private lateinit var accountRepository: AccountRepository
    private lateinit var budgetRepository: BudgetRepository
    private lateinit var userRepository: UserRepository
    private lateinit var accountMapper: AccountMapper
    private lateinit var accountService: AccountService

    private lateinit var securityContext: SecurityContext
    private lateinit var authentication: Authentication

    private val userId = UUID.randomUUID()
    private val budgetId = UUID.randomUUID()
    private val accountId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        accountRepository = mockk()
        budgetRepository = mockk()
        userRepository = mockk()
        accountMapper = mockk()

        accountService = AccountService(
            accountRepository = accountRepository,
            budgetRepository = budgetRepository,
            userRepository = userRepository,
            accountMapper = accountMapper
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
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
        SecurityContextHolder.clearContext()
    }

    // ==================== CREATE ACCOUNT TESTS ====================

    @Test
    fun `createAccount should create new account when user has access to budget`() {
        // Given
        val owner = createUser(userId, "owner@example.com")
        val budget = createBudget(budgetId, "My Budget", owner)
        budget.addMember(owner, MemberRole.OWNER)

        val request = CreateAccountRequest(
            name = "Checking Account",
            type = AccountType.CHECKING,
            currency = "USD",
            openingBalance = BigDecimal("1000.00")
        )

        val account = Account(
            budget = budget,
            name = request.name,
            type = request.type,
            currency = request.currency,
            openingBalance = request.openingBalance,
            currentBalance = request.openingBalance,
            active = true,
            id = accountId
        )

        every { budgetRepository.findById(budgetId) } returns budget
        every { userRepository.findById(userId) } returns owner
        every { accountRepository.save(any<Account>()) } returns account
        every { accountMapper.toResponse(account) } returns mockk()

        // When
        accountService.createAccount(budgetId, request)

        // Then
        verify(exactly = 1) { budgetRepository.findById(budgetId) }
        verify(exactly = 1) { accountRepository.save(any<Account>()) }
        verify(exactly = 1) { accountMapper.toResponse(account) }
    }

    @Test
    fun `createAccount should throw EntityNotFoundException when budget not found`() {
        // Given
        val request = CreateAccountRequest(
            name = "Checking",
            type = AccountType.CHECKING,
            currency = "USD",
            openingBalance = BigDecimal.ZERO
        )

        every { budgetRepository.findById(budgetId) } returns null

        // When & Then
        assertThrows<EntityNotFoundException> {
            accountService.createAccount(budgetId, request)
        }

        verify(exactly = 1) { budgetRepository.findById(budgetId) }
        verify(exactly = 0) { accountRepository.save(any<Account>()) }
    }

    @Test
    fun `createAccount should throw AccessDeniedException when user has no access`() {
        // Given
        val otherOwnerId = UUID.randomUUID()
        val otherOwner = createUser(otherOwnerId, "other@example.com")
        val currentUser = createUser(userId, "user@example.com")
        val budget = createBudget(budgetId, "Other Budget", otherOwner)

        val request = CreateAccountRequest(
            name = "Checking",
            type = AccountType.CHECKING,
            currency = "USD",
            openingBalance = BigDecimal.ZERO
        )

        every { budgetRepository.findById(budgetId) } returns budget
        every { userRepository.findById(userId) } returns currentUser

        // When & Then
        assertThrows<AccessDeniedException> {
            accountService.createAccount(budgetId, request)
        }

        verify(exactly = 0) { accountRepository.save(any<Account>()) }
    }

    // ==================== GET ACCOUNTS TESTS ====================

    @Test
    fun `getAccountsByBudget should return all accounts for budget`() {
        // Given
        val owner = createUser(userId, "owner@example.com")
        val budget = createBudget(budgetId, "My Budget", owner)
        budget.addMember(owner, MemberRole.OWNER)

        val account1 = Account(
            budget = budget,
            name = "Checking",
            type = AccountType.CHECKING,
            currentBalance = BigDecimal("1000.00"),
            active = true,
            id = UUID.randomUUID()
        )

        val account2 = Account(
            budget = budget,
            name = "Savings",
            type = AccountType.SAVINGS,
            currentBalance = BigDecimal("5000.00"),
            active = true,
            id = UUID.randomUUID()
        )

        val accounts = listOf(account1, account2)

        every { budgetRepository.findById(budgetId) } returns budget
        every { userRepository.findById(userId) } returns owner
        every { accountRepository.findByBudgetId(budgetId) } returns accounts
        every { accountMapper.toResponse(any()) } returns mockk()

        // When
        val responses = accountService.getAccountsByBudget(budgetId)

        // Then
        assertNotNull(responses)
        assertEquals(2, responses.size)
        verify(exactly = 1) { accountRepository.findByBudgetId(budgetId) }
        verify(exactly = 2) { accountMapper.toResponse(any()) }
    }

    // ==================== GET ACCOUNT BY ID TESTS ====================

    @Test
    fun `getAccountById should return account when user has access`() {
        // Given
        val owner = createUser(userId, "owner@example.com")
        val budget = createBudget(budgetId, "My Budget", owner)
        budget.addMember(owner, MemberRole.OWNER)

        val account = Account(
            budget = budget,
            name = "Checking",
            type = AccountType.CHECKING,
            currentBalance = BigDecimal("1000.00"),
            active = true,
            id = accountId
        )

        every { accountRepository.findById(accountId) } returns account
        every { budgetRepository.findById(budgetId) } returns budget
        every { userRepository.findById(userId) } returns owner
        every { accountMapper.toResponse(account) } returns mockk()

        // When
        accountService.getAccountById(accountId)

        // Then
        verify(exactly = 1) { accountRepository.findById(accountId) }
        verify(exactly = 1) { accountMapper.toResponse(account) }
    }

    @Test
    fun `getAccountById should throw EntityNotFoundException when account not found`() {
        // Given
        every { accountRepository.findById(accountId) } returns null

        // When & Then
        assertThrows<EntityNotFoundException> {
            accountService.getAccountById(accountId)
        }

        verify(exactly = 1) { accountRepository.findById(accountId) }
    }

    // ==================== UPDATE ACCOUNT TESTS ====================

    @Test
    fun `updateAccount should update account when user has access`() {
        // Given
        val owner = createUser(userId, "owner@example.com")
        val budget = createBudget(budgetId, "My Budget", owner)
        budget.addMember(owner, MemberRole.OWNER)

        val account = Account(
            budget = budget,
            name = "Old Name",
            type = AccountType.CHECKING,
            currentBalance = BigDecimal("1000.00"),
            active = true,
            id = accountId
        )

        val request = UpdateAccountRequest(
            name = "New Name",
            currency = "USD"
        )

        every { accountRepository.findById(accountId) } returns account
        every { budgetRepository.findById(budgetId) } returns budget
        every { userRepository.findById(userId) } returns owner
        every { accountRepository.save(any<Account>()) } returns account
        every { accountMapper.toResponse(any()) } returns mockk()

        // When
        accountService.updateAccount(accountId, request)

        // Then
        verify(exactly = 1) { accountRepository.findById(accountId) }
        verify(exactly = 1) { accountRepository.save(any<Account>()) }
    }

    // ==================== DELETE ACCOUNT TESTS ====================

    @Test
    fun `deleteAccount should delete account when user has access and balance is zero`() {
        // Given
        val owner = createUser(userId, "owner@example.com")
        val budget = createBudget(budgetId, "My Budget", owner)
        budget.addMember(owner, MemberRole.OWNER)

        val account = Account(
            budget = budget,
            name = "Checking",
            type = AccountType.CHECKING,
            currentBalance = BigDecimal.ZERO,
            active = true,
            id = accountId
        )

        every { accountRepository.findById(accountId) } returns account
        every { budgetRepository.findById(budgetId) } returns budget
        every { userRepository.findById(userId) } returns owner
        every { accountRepository.delete(account) } just Runs

        // When
        accountService.deleteAccount(accountId)

        // Then
        verify(exactly = 1) { accountRepository.findById(accountId) }
        verify(exactly = 1) { accountRepository.delete(account) }
    }

    @Test
    fun `deleteAccount should throw InvalidOperationException when balance is non-zero`() {
        // Given
        val owner = createUser(userId, "owner@example.com")
        val budget = createBudget(budgetId, "My Budget", owner)
        budget.addMember(owner, MemberRole.OWNER)

        val account = Account(
            budget = budget,
            name = "Checking",
            type = AccountType.CHECKING,
            currentBalance = BigDecimal("1000.00"),
            active = true,
            id = accountId
        )

        every { accountRepository.findById(accountId) } returns account
        every { budgetRepository.findById(budgetId) } returns budget
        every { userRepository.findById(userId) } returns owner

        // When & Then
        val exception = assertThrows<InvalidOperationException> {
            accountService.deleteAccount(accountId)
        }

        assertEquals("Cannot delete account with non-zero balance", exception.message)
        verify(exactly = 0) { accountRepository.delete(any()) }
    }

    // ==================== TRANSFER TESTS ====================

    @Test
    fun `transfer should transfer money between accounts`() {
        // Given
        val owner = createUser(userId, "owner@example.com")
        val budget = createBudget(budgetId, "My Budget", owner)
        budget.addMember(owner, MemberRole.OWNER)

        val fromAccountId = UUID.randomUUID()
        val toAccountId = UUID.randomUUID()

        val fromAccount = Account(
            budget = budget,
            name = "Checking",
            type = AccountType.CHECKING,
            currentBalance = BigDecimal("1000.00"),
            active = true,
            id = fromAccountId
        )

        val toAccount = Account(
            budget = budget,
            name = "Savings",
            type = AccountType.SAVINGS,
            currentBalance = BigDecimal("5000.00"),
            active = true,
            id = toAccountId
        )

        val request = TransferRequest(
            fromAccountId = fromAccountId,
            toAccountId = toAccountId,
            amount = BigDecimal("500.00"),
            description = "Transfer to savings"
        )

        every { accountRepository.findById(fromAccountId) } returns fromAccount
        every { accountRepository.findById(toAccountId) } returns toAccount
        every { budgetRepository.findById(budgetId) } returns budget
        every { userRepository.findById(userId) } returns owner
        every { accountRepository.save(any<Account>()) } returnsArgument 0

        // When
        accountService.transfer(request)

        // Then
        verify(exactly = 1) { accountRepository.findById(fromAccountId) }
        verify(exactly = 1) { accountRepository.findById(toAccountId) }
        verify(exactly = 2) { accountRepository.save(any<Account>()) }
    }

    @Test
    fun `transfer should throw InvalidOperationException when amount is not positive`() {
        // Given
        val owner = createUser(userId, "owner@example.com")
        val budget = createBudget(budgetId, "My Budget", owner)
        budget.addMember(owner, MemberRole.OWNER)

        val fromAccountId = UUID.randomUUID()
        val toAccountId = UUID.randomUUID()

        val fromAccount = Account(
            budget = budget,
            name = "Checking",
            type = AccountType.CHECKING,
            currentBalance = BigDecimal("1000.00"),
            active = true,
            id = fromAccountId
        )

        val toAccount = Account(
            budget = budget,
            name = "Savings",
            type = AccountType.SAVINGS,
            currentBalance = BigDecimal("5000.00"),
            active = true,
            id = toAccountId
        )

        val request = TransferRequest(
            fromAccountId = fromAccountId,
            toAccountId = toAccountId,
            amount = BigDecimal("-100.00"),
            description = "Invalid transfer"
        )

        every { accountRepository.findById(fromAccountId) } returns fromAccount
        every { accountRepository.findById(toAccountId) } returns toAccount
        every { budgetRepository.findById(budgetId) } returns budget
        every { userRepository.findById(userId) } returns owner

        // When & Then
        val exception = assertThrows<InvalidOperationException> {
            accountService.transfer(request)
        }

        assertEquals("Transfer amount must be positive", exception.message)
    }

    @Test
    fun `transfer should throw InvalidOperationException when insufficient balance`() {
        // Given
        val owner = createUser(userId, "owner@example.com")
        val budget = createBudget(budgetId, "My Budget", owner)
        budget.addMember(owner, MemberRole.OWNER)

        val fromAccountId = UUID.randomUUID()
        val toAccountId = UUID.randomUUID()

        val fromAccount = Account(
            budget = budget,
            name = "Checking",
            type = AccountType.CHECKING,
            currentBalance = BigDecimal("100.00"),
            active = true,
            id = fromAccountId
        )

        val toAccount = Account(
            budget = budget,
            name = "Savings",
            type = AccountType.SAVINGS,
            currentBalance = BigDecimal("5000.00"),
            active = true,
            id = toAccountId
        )

        val request = TransferRequest(
            fromAccountId = fromAccountId,
            toAccountId = toAccountId,
            amount = BigDecimal("500.00"),
            description = "Transfer"
        )

        every { accountRepository.findById(fromAccountId) } returns fromAccount
        every { accountRepository.findById(toAccountId) } returns toAccount
        every { budgetRepository.findById(budgetId) } returns budget
        every { userRepository.findById(userId) } returns owner

        // When & Then
        val exception = assertThrows<InvalidOperationException> {
            accountService.transfer(request)
        }

        assertEquals("Insufficient balance in source account", exception.message)
    }

    // ==================== ACTIVATE/DEACTIVATE TESTS ====================

    @Test
    fun `activateAccount should activate inactive account`() {
        // Given
        val owner = createUser(userId, "owner@example.com")
        val budget = createBudget(budgetId, "My Budget", owner)
        budget.addMember(owner, MemberRole.OWNER)

        val account = Account(
            budget = budget,
            name = "Checking",
            type = AccountType.CHECKING,
            currentBalance = BigDecimal("1000.00"),
            active = false,
            id = accountId
        )

        every { accountRepository.findById(accountId) } returns account
        every { budgetRepository.findById(budgetId) } returns budget
        every { userRepository.findById(userId) } returns owner
        every { accountRepository.save(any<Account>()) } returns account
        every { accountMapper.toResponse(any()) } returns mockk()

        // When
        accountService.activateAccount(accountId)

        // Then
        verify(exactly = 1) { accountRepository.findById(accountId) }
        verify(exactly = 1) { accountRepository.save(any<Account>()) }
    }

    @Test
    fun `deactivateAccount should deactivate active account`() {
        // Given
        val owner = createUser(userId, "owner@example.com")
        val budget = createBudget(budgetId, "My Budget", owner)
        budget.addMember(owner, MemberRole.OWNER)

        val account = Account(
            budget = budget,
            name = "Checking",
            type = AccountType.CHECKING,
            currentBalance = BigDecimal("1000.00"),
            active = true,
            id = accountId
        )

        every { accountRepository.findById(accountId) } returns account
        every { budgetRepository.findById(budgetId) } returns budget
        every { userRepository.findById(userId) } returns owner
        every { accountRepository.save(any<Account>()) } returns account
        every { accountMapper.toResponse(any()) } returns mockk()

        // When
        accountService.deactivateAccount(accountId)

        // Then
        verify(exactly = 1) { accountRepository.findById(accountId) }
        verify(exactly = 1) { accountRepository.save(any<Account>()) }
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
