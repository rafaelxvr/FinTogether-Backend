package com.fintogether.application.service

import com.fintogether.application.dto.*
import com.fintogether.domain.entity.*
import com.fintogether.domain.exception.AccessDeniedException
import com.fintogether.domain.exception.EntityNotFoundException
import com.fintogether.domain.exception.InvalidOperationException
import com.fintogether.domain.repository.AccountRepository
import com.fintogether.domain.repository.BudgetRepository
import com.fintogether.domain.repository.CreditCardRepository
import io.mockk.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.util.*

class CreditCardServiceTest {

    private lateinit var creditCardRepository: CreditCardRepository
    private lateinit var budgetRepository: BudgetRepository
    private lateinit var accountRepository: AccountRepository
    private lateinit var creditCardService: CreditCardService

    private val userId = UUID.randomUUID()
    private val budgetId = UUID.randomUUID()
    private val cardId = UUID.randomUUID()

    private lateinit var owner: User
    private lateinit var budget: Budget

    @BeforeEach
    fun setUp() {
        creditCardRepository = mockk()
        budgetRepository = mockk()
        accountRepository = mockk()

        creditCardService = CreditCardService(
            creditCardRepository = creditCardRepository,
            budgetRepository = budgetRepository,
            accountRepository = accountRepository
        )

        owner = createUser(userId, "owner@example.com")
        budget = createBudget(budgetId, "My Budget", owner)
        budget.addMember(owner, MemberRole.OWNER)
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    // ==================== CREATE CREDIT CARD TESTS ====================

    @Test
    fun `createCreditCard should create card when user is budget member`() {
        // Given
        val request = CreateCreditCardRequest(
            budgetId = budgetId.toString(),
            name = "Visa Platinum",
            issuer = "Bank of America",
            lastFour = "1234",
            currency = "USD",
            statementCloseDay = 15,
            dueDay = 25,
            linkedAccountId = null
        )

        val creditCard = CreditCard(
            budget = budget,
            name = request.name,
            issuer = request.issuer,
            lastFour = request.lastFour,
            currency = request.currency,
            statementCloseDay = request.statementCloseDay,
            dueDay = request.dueDay,
            id = cardId
        )

        every { budgetRepository.findById(budgetId) } returns budget
        every { creditCardRepository.save(any<CreditCard>()) } returns creditCard

        // When
        val response = creditCardService.createCreditCard(request, userId)

        // Then
        assertNotNull(response)
        assertEquals(cardId.toString(), response.id)
        assertEquals("Visa Platinum", response.name)
        verify(exactly = 1) { creditCardRepository.save(any<CreditCard>()) }
    }

    @Test
    fun `createCreditCard should create card with linked account`() {
        // Given
        val accountId = UUID.randomUUID()
        val account = Account(
            budget = budget,
            name = "Checking",
            type = AccountType.CHECKING,
            currentBalance = BigDecimal("1000.00"),
            active = true,
            id = accountId
        )

        val request = CreateCreditCardRequest(
            budgetId = budgetId.toString(),
            name = "Visa",
            statementCloseDay = 15,
            dueDay = 25,
            linkedAccountId = accountId.toString()
        )

        val creditCard = CreditCard(
            budget = budget,
            name = request.name,
            statementCloseDay = request.statementCloseDay,
            dueDay = request.dueDay,
            linkedAccount = account,
            id = cardId
        )

        every { budgetRepository.findById(budgetId) } returns budget
        every { accountRepository.findById(accountId) } returns account
        every { creditCardRepository.save(any<CreditCard>()) } returns creditCard

        // When
        val response = creditCardService.createCreditCard(request, userId)

        // Then
        assertNotNull(response)
        assertEquals(accountId.toString(), response.linkedAccountId)
    }

    @Test
    fun `createCreditCard should throw EntityNotFoundException when budget not found`() {
        // Given
        val request = CreateCreditCardRequest(
            budgetId = budgetId.toString(),
            name = "Visa Platinum",
            statementCloseDay = 15,
            dueDay = 25
        )

        every { budgetRepository.findById(budgetId) } returns null

        // When & Then
        assertThrows<EntityNotFoundException> {
            creditCardService.createCreditCard(request, userId)
        }

        verify(exactly = 0) { creditCardRepository.save(any<CreditCard>()) }
    }

    @Test
    fun `createCreditCard should throw AccessDeniedException when user is not member`() {
        // Given
        val nonMemberId = UUID.randomUUID()
        val request = CreateCreditCardRequest(
            budgetId = budgetId.toString(),
            name = "Visa",
            statementCloseDay = 15,
            dueDay = 25
        )

        every { budgetRepository.findById(budgetId) } returns budget

        // When & Then
        assertThrows<AccessDeniedException> {
            creditCardService.createCreditCard(request, nonMemberId)
        }
    }

    @Test
    fun `createCreditCard should throw InvalidOperationException when linked account belongs to different budget`() {
        // Given
        val otherBudget = createBudget(UUID.randomUUID(), "Other", owner)
        val accountId = UUID.randomUUID()
        val account = Account(
            budget = otherBudget,
            name = "Other Checking",
            type = AccountType.CHECKING,
            currentBalance = BigDecimal.ZERO,
            active = true,
            id = accountId
        )

        val request = CreateCreditCardRequest(
            budgetId = budgetId.toString(),
            name = "Visa",
            statementCloseDay = 15,
            dueDay = 25,
            linkedAccountId = accountId.toString()
        )

        every { budgetRepository.findById(budgetId) } returns budget
        every { accountRepository.findById(accountId) } returns account

        // When & Then
        val exception = assertThrows<InvalidOperationException> {
            creditCardService.createCreditCard(request, userId)
        }

        assertEquals("Account must belong to the same budget", exception.message)
    }

    // ==================== GET CREDIT CARD TESTS ====================

    @Test
    fun `getCreditCardById should return card when user has access`() {
        // Given
        val creditCard = CreditCard(
            budget = budget,
            name = "Visa",
            statementCloseDay = 15,
            dueDay = 25,
            id = cardId
        )

        every { creditCardRepository.findById(cardId) } returns creditCard

        // When
        val response = creditCardService.getCreditCardById(cardId, userId)

        // Then
        assertNotNull(response)
        assertEquals(cardId.toString(), response.id)
    }

    @Test
    fun `getCreditCardById should throw EntityNotFoundException when card not found`() {
        // Given
        every { creditCardRepository.findById(cardId) } returns null

        // When & Then
        assertThrows<EntityNotFoundException> {
            creditCardService.getCreditCardById(cardId, userId)
        }

        verify(exactly = 1) { creditCardRepository.findById(cardId) }
    }

    @Test
    fun `getCreditCardsByBudget should return all cards for budget`() {
        // Given
        val card1 = CreditCard(budget = budget, name = "Visa", statementCloseDay = 15, dueDay = 25, id = UUID.randomUUID())
        val card2 = CreditCard(budget = budget, name = "Mastercard", statementCloseDay = 10, dueDay = 20, id = UUID.randomUUID())

        every { budgetRepository.findById(budgetId) } returns budget
        every { creditCardRepository.findByBudgetId(budgetId) } returns listOf(card1, card2)

        // When
        val result = creditCardService.getCreditCardsByBudget(budgetId, userId)

        // Then
        assertEquals(2, result.size)
    }

    // ==================== DELETE CREDIT CARD TESTS ====================

    @Test
    fun `deleteCreditCard should delete card when user has access`() {
        // Given
        val creditCard = CreditCard(
            budget = budget,
            name = "Visa",
            statementCloseDay = 15,
            dueDay = 25,
            id = cardId
        )

        every { creditCardRepository.findById(cardId) } returns creditCard
        every { creditCardRepository.delete(creditCard) } just Runs

        // When
        creditCardService.deleteCreditCard(cardId, userId)

        // Then
        verify(exactly = 1) { creditCardRepository.delete(creditCard) }
    }

    // ==================== TOGGLE STATUS TESTS ====================

    @Test
    fun `toggleStatus should activate card`() {
        // Given
        val creditCard = CreditCard(
            budget = budget,
            name = "Visa",
            statementCloseDay = 15,
            dueDay = 25,
            active = false,
            id = cardId
        )

        val request = ToggleCreditCardStatusRequest(active = true)

        every { creditCardRepository.findById(cardId) } returns creditCard
        every { creditCardRepository.save(any<CreditCard>()) } returnsArgument 0

        // When
        val response = creditCardService.toggleStatus(cardId, request, userId)

        // Then
        verify(exactly = 1) { creditCardRepository.save(any<CreditCard>()) }
    }

    @Test
    fun `toggleStatus should deactivate card`() {
        // Given
        val creditCard = CreditCard(
            budget = budget,
            name = "Visa",
            statementCloseDay = 15,
            dueDay = 25,
            active = true,
            id = cardId
        )

        val request = ToggleCreditCardStatusRequest(active = false)

        every { creditCardRepository.findById(cardId) } returns creditCard
        every { creditCardRepository.save(any<CreditCard>()) } returnsArgument 0

        // When
        creditCardService.toggleStatus(cardId, request, userId)

        // Then
        assertFalse(creditCard.active)
        verify(exactly = 1) { creditCardRepository.save(any<CreditCard>()) }
    }

    // ==================== LINK/UNLINK ACCOUNT TESTS ====================

    @Test
    fun `linkAccount should link account to credit card`() {
        // Given
        val accountId = UUID.randomUUID()
        val account = Account(
            budget = budget,
            name = "Checking",
            type = AccountType.CHECKING,
            currentBalance = BigDecimal("1000.00"),
            active = true,
            id = accountId
        )

        val creditCard = CreditCard(
            budget = budget,
            name = "Visa",
            statementCloseDay = 15,
            dueDay = 25,
            id = cardId
        )

        val request = LinkAccountRequest(accountId = accountId.toString())

        every { creditCardRepository.findById(cardId) } returns creditCard
        every { accountRepository.findById(accountId) } returns account
        every { creditCardRepository.save(any<CreditCard>()) } returnsArgument 0

        // When
        val response = creditCardService.linkAccount(cardId, request, userId)

        // Then
        assertEquals(accountId.toString(), response.linkedAccountId)
    }

    @Test
    fun `unlinkAccount should remove linked account from credit card`() {
        // Given
        val accountId = UUID.randomUUID()
        val account = Account(
            budget = budget,
            name = "Checking",
            type = AccountType.CHECKING,
            currentBalance = BigDecimal("1000.00"),
            active = true,
            id = accountId
        )

        val creditCard = CreditCard(
            budget = budget,
            name = "Visa",
            statementCloseDay = 15,
            dueDay = 25,
            linkedAccount = account,
            id = cardId
        )

        every { creditCardRepository.findById(cardId) } returns creditCard
        every { creditCardRepository.save(any<CreditCard>()) } returnsArgument 0

        // When
        val response = creditCardService.unlinkAccount(cardId, userId)

        // Then
        assertNull(response.linkedAccountId)
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
