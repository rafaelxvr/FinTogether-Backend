package com.fintogether.application.service

import com.fintogether.application.dto.CategoryAllocationInput
import com.fintogether.application.dto.SetAllocationsRequest
import com.fintogether.domain.entity.*
import com.fintogether.domain.exception.AccessDeniedException
import com.fintogether.domain.repository.*
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BudgetAllocationServiceTest {

    private lateinit var budgetAllocationRepository: BudgetAllocationRepository
    private lateinit var budgetRepository: BudgetRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var userRepository: UserRepository
    private lateinit var service: BudgetAllocationService

    private lateinit var testUser: User
    private lateinit var testBudget: Budget
    private lateinit var testCategory: Category
    private val userId = UUID.randomUUID()
    private val budgetId = UUID.randomUUID()
    private val categoryId = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        budgetAllocationRepository = mockk()
        budgetRepository = mockk()
        categoryRepository = mockk()
        transactionRepository = mockk()
        userRepository = mockk()

        service = BudgetAllocationService(
            budgetAllocationRepository,
            budgetRepository,
            categoryRepository,
            transactionRepository,
            userRepository
        )

        testUser = mockk {
            every { id } returns userId
        }
        testBudget = mockk {
            every { id } returns budgetId
            every { hasMember(any()) } returns true
            every { isOwner(any()) } returns true
        }
        testCategory = mockk {
            every { id } returns categoryId
            every { name } returns "Food"
            every { icon } returns null
            every { color } returns null
            every { limitAmount } returns null
        }

        every { budgetRepository.findById(budgetId) } returns testBudget
        every { userRepository.findById(userId) } returns testUser
        every { categoryRepository.findById(categoryId) } returns testCategory
    }

    @Test
    fun `getAllocations should return correct percentages`() {
        val month = YearMonth.of(2026, 3)
        val allocation = BudgetAllocation(
            budget = testBudget,
            category = testCategory,
            month = month.atDay(1),
            allocatedAmount = BigDecimal("1000.00")
        )

        every { budgetAllocationRepository.findByBudgetIdAndMonth(budgetId, month.atDay(1)) } returns listOf(allocation)

        val transaction = mockk<Transaction> {
            every { type } returns TransactionType.EXPENSE
            every { category } returns testCategory
            every { amount } returns BigDecimal("300.00")
        }
        every { transactionRepository.findByBudgetIdAndDateBetween(budgetId, any(), any()) } returns listOf(transaction)

        val result = service.getAllocations(budgetId, month, userId)

        assertEquals(1, result.categories.size)
        assertEquals(30.0, result.categories[0].percentageUsed, 0.1)
        assertEquals(BigDecimal("300.00"), result.categories[0].spentAmount)
    }

    @Test
    fun `setAllocations should create new allocation`() {
        val month = YearMonth.of(2026, 3)
        val request = SetAllocationsRequest(
            month = month,
            allocations = listOf(
                CategoryAllocationInput(categoryId = categoryId, allocatedAmount = BigDecimal("1000.00"))
            )
        )

        every { budgetAllocationRepository.findByBudgetIdAndCategoryIdAndMonth(budgetId, categoryId, month.atDay(1)) } returns null
        every { budgetAllocationRepository.save(any()) } answers { firstArg() }
        every { budgetAllocationRepository.findByBudgetIdAndMonth(budgetId, month.atDay(1)) } returns emptyList()
        every { transactionRepository.findByBudgetIdAndDateBetween(budgetId, any(), any()) } returns emptyList()

        val result = service.setAllocations(budgetId, request, userId)

        verify { budgetAllocationRepository.save(any()) }
        assertEquals(budgetId.toString(), result.budgetId)
    }

    @Test
    fun `setAllocations should update existing allocation`() {
        val month = YearMonth.of(2026, 3)
        val existingAllocation = BudgetAllocation(
            budget = testBudget,
            category = testCategory,
            month = month.atDay(1),
            allocatedAmount = BigDecimal("500.00")
        )
        val request = SetAllocationsRequest(
            month = month,
            allocations = listOf(
                CategoryAllocationInput(categoryId = categoryId, allocatedAmount = BigDecimal("1000.00"))
            )
        )

        every { budgetAllocationRepository.findByBudgetIdAndCategoryIdAndMonth(budgetId, categoryId, month.atDay(1)) } returns existingAllocation
        every { budgetAllocationRepository.save(any()) } answers { firstArg() }
        every { budgetAllocationRepository.findByBudgetIdAndMonth(budgetId, month.atDay(1)) } returns listOf(existingAllocation)
        every { transactionRepository.findByBudgetIdAndDateBetween(budgetId, any(), any()) } returns emptyList()

        service.setAllocations(budgetId, request, userId)

        assertEquals(BigDecimal("1000.00"), existingAllocation.allocatedAmount)
    }

    @Test
    fun `setAllocations should reject allocation exceeding category limit`() {
        val month = YearMonth.of(2026, 3)
        val categoryWithLimit = mockk<Category> {
            every { id } returns categoryId
            every { name } returns "Food"
            every { icon } returns null
            every { color } returns null
            every { limitAmount } returns BigDecimal("800.00")
        }
        every { categoryRepository.findById(categoryId) } returns categoryWithLimit

        val request = SetAllocationsRequest(
            month = month,
            allocations = listOf(
                CategoryAllocationInput(categoryId = categoryId, allocatedAmount = BigDecimal("900.00"))
            )
        )

        every { budgetAllocationRepository.findByBudgetIdAndCategoryIdAndMonth(budgetId, categoryId, month.atDay(1)) } returns null

        assertThrows<IllegalArgumentException> {
            service.setAllocations(budgetId, request, userId)
        }
    }
}
