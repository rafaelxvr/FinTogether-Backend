package com.fintogether.domain.entity

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class BudgetAllocationTest {

    private fun createBudget(): Budget {
        val owner = User(name = "Test User", email = "test@test.com", passwordHash = "hash")
        return Budget(name = "Test Budget", owner = owner)
    }

    private fun createCategory(budget: Budget, limitAmount: BigDecimal? = null): Category {
        return Category(
            budget = budget,
            name = "Food",
            type = CategoryType.EXPENSE,
            limitAmount = limitAmount
        )
    }

    @Test
    fun `updateAmount with valid amount should update amount and updatedAt`() {
        val budget = createBudget()
        val category = createCategory(budget)
        val allocation = BudgetAllocation(
            budget = budget,
            category = category,
            month = LocalDate.of(2026, 3, 1),
            allocatedAmount = BigDecimal("1000.00")
        )

        allocation.updateAmount(BigDecimal("1500.00"))

        assertEquals(BigDecimal("1500.00"), allocation.allocatedAmount)
        assertNotNull(allocation.updatedAt)
    }

    @Test
    fun `updateAmount with zero amount should succeed`() {
        val budget = createBudget()
        val category = createCategory(budget)
        val allocation = BudgetAllocation(
            budget = budget,
            category = category,
            month = LocalDate.of(2026, 3, 1),
            allocatedAmount = BigDecimal("1000.00")
        )

        allocation.updateAmount(BigDecimal.ZERO)

        assertEquals(BigDecimal.ZERO, allocation.allocatedAmount)
    }

    @Test
    fun `updateAmount with negative amount should throw`() {
        val budget = createBudget()
        val category = createCategory(budget)
        val allocation = BudgetAllocation(
            budget = budget,
            category = category,
            month = LocalDate.of(2026, 3, 1),
            allocatedAmount = BigDecimal("1000.00")
        )

        assertThrows<IllegalArgumentException> {
            allocation.updateAmount(BigDecimal("-100.00"))
        }
    }

    @Test
    fun `validateAgainstLimit should pass when allocation within limit`() {
        val budget = createBudget()
        val category = createCategory(budget, limitAmount = BigDecimal("2000.00"))
        val allocation = BudgetAllocation(
            budget = budget,
            category = category,
            month = LocalDate.of(2026, 3, 1),
            allocatedAmount = BigDecimal("1500.00")
        )

        // Should not throw
        allocation.validateAgainstLimit()
    }

    @Test
    fun `validateAgainstLimit should throw when allocation exceeds limit`() {
        val budget = createBudget()
        val category = createCategory(budget, limitAmount = BigDecimal("1000.00"))
        val allocation = BudgetAllocation(
            budget = budget,
            category = category,
            month = LocalDate.of(2026, 3, 1),
            allocatedAmount = BigDecimal("1500.00")
        )

        assertThrows<IllegalArgumentException> {
            allocation.validateAgainstLimit()
        }
    }

    @Test
    fun `validateAgainstLimit should pass when no limit is set`() {
        val budget = createBudget()
        val category = createCategory(budget, limitAmount = null)
        val allocation = BudgetAllocation(
            budget = budget,
            category = category,
            month = LocalDate.of(2026, 3, 1),
            allocatedAmount = BigDecimal("5000.00")
        )

        // Should not throw
        allocation.validateAgainstLimit()
    }

    @Test
    fun `equals should compare by id`() {
        val budget = createBudget()
        val category = createCategory(budget)
        val id = UUID.randomUUID()
        val now = LocalDateTime.now()

        val a1 = BudgetAllocation(
            budget = budget, category = category,
            month = LocalDate.of(2026, 3, 1),
            allocatedAmount = BigDecimal("100.00"),
            id = id, createdAt = now
        )
        val a2 = BudgetAllocation(
            budget = budget, category = category,
            month = LocalDate.of(2026, 3, 1),
            allocatedAmount = BigDecimal("200.00"),
            id = id, createdAt = now
        )

        assertEquals(a1, a2)
        assertEquals(a1.hashCode(), a2.hashCode())
    }
}
