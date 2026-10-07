package com.fintogether.application.service

import com.fintogether.application.dto.*
import com.fintogether.domain.entity.BudgetAllocation
import com.fintogether.domain.entity.TransactionType
import com.fintogether.domain.exception.AccessDeniedException
import com.fintogether.domain.exception.EntityNotFoundException
import com.fintogether.domain.repository.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.YearMonth
import java.util.*

/**
 * Service for managing budget allocations per category.
 * Handles CRUD operations and computes actual spending vs allocation data.
 */
@Service
class BudgetAllocationService(
    private val budgetAllocationRepository: BudgetAllocationRepository,
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val userRepository: UserRepository
) {

    private val logger = LoggerFactory.getLogger(BudgetAllocationService::class.java)

    /**
     * Get allocations with actual spending for a budget and month.
     */
    @Transactional(readOnly = true)
    fun getAllocations(budgetId: UUID, yearMonth: YearMonth, userId: UUID): BudgetAllocationsResponse {
        logger.debug("Getting allocations for budget: $budgetId, month: $yearMonth")
        validateBudgetAccess(budgetId, userId)
        val month = yearMonth.atDay(1)

        val allocations = budgetAllocationRepository.findByBudgetIdAndMonth(budgetId, month)

        // Get actual spending per category for the month
        val startDate = yearMonth.atDay(1)
        val endDate = yearMonth.atEndOfMonth()
        val transactions = transactionRepository.findByBudgetIdAndDateBetween(budgetId, startDate, endDate)
        val spendingByCategory = transactions
            .filter { it.type == TransactionType.EXPENSE && it.category != null }
            .groupBy { it.category!!.id }
            .mapValues { (_, txns) -> txns.sumOf { it.amount } }

        val categoryResponses = allocations.map { allocation ->
            val spent = spendingByCategory[allocation.category.id] ?: BigDecimal.ZERO
            val remaining = allocation.allocatedAmount.subtract(spent)
            val percentageUsed = if (allocation.allocatedAmount > BigDecimal.ZERO) {
                spent.divide(allocation.allocatedAmount, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).toDouble()
            } else 0.0

            CategoryAllocationResponse(
                categoryId = allocation.category.id.toString(),
                categoryName = allocation.category.name,
                categoryIcon = allocation.category.icon,
                categoryColor = allocation.category.color,
                allocatedAmount = allocation.allocatedAmount,
                spentAmount = spent,
                remainingAmount = remaining,
                percentageUsed = percentageUsed,
                limitAmount = allocation.category.limitAmount,
                isOverBudget = spent > allocation.allocatedAmount
            )
        }

        val totalAllocated = allocations.sumOf { it.allocatedAmount }
        val totalSpent = categoryResponses.sumOf { it.spentAmount }
        val totalRemaining = totalAllocated.subtract(totalSpent)
        val overallPercentage = if (totalAllocated > BigDecimal.ZERO) {
            totalSpent.divide(totalAllocated, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).toDouble()
        } else 0.0

        return BudgetAllocationsResponse(
            budgetId = budgetId.toString(),
            month = yearMonth.toString(),
            totalAllocated = totalAllocated,
            totalSpent = totalSpent,
            totalRemaining = totalRemaining,
            overallPercentageUsed = overallPercentage,
            categories = categoryResponses
        )
    }

    /**
     * Set or update allocations for multiple categories in a single month.
     */
    @Transactional
    fun setAllocations(budgetId: UUID, request: SetAllocationsRequest, userId: UUID): BudgetAllocationsResponse {
        logger.debug("Setting allocations for budget: $budgetId, month: ${request.month}")
        validateBudgetAccess(budgetId, userId)
        val month = request.month.atDay(1)

        for (input in request.allocations) {
            val existing = budgetAllocationRepository
                .findByBudgetIdAndCategoryIdAndMonth(budgetId, input.categoryId, month)

            if (existing != null) {
                existing.updateAmount(input.allocatedAmount)
                existing.validateAgainstLimit()
                budgetAllocationRepository.save(existing)
            } else {
                val budget = budgetRepository.findById(budgetId)
                    ?: throw EntityNotFoundException("Budget", budgetId)
                val category = categoryRepository.findById(input.categoryId)
                    ?: throw EntityNotFoundException("Category", input.categoryId)
                val allocation = BudgetAllocation(
                    budget = budget,
                    category = category,
                    month = month,
                    allocatedAmount = input.allocatedAmount
                )
                allocation.validateAgainstLimit()
                budgetAllocationRepository.save(allocation)
            }
        }

        return getAllocations(budgetId, request.month, userId)
    }

    /**
     * Get budget health summary for a budget and month.
     */
    @Transactional(readOnly = true)
    fun getBudgetHealth(budgetId: UUID, yearMonth: YearMonth, userId: UUID): BudgetHealthResponse {
        val allocations = getAllocations(budgetId, yearMonth, userId)

        val status = when {
            allocations.overallPercentageUsed > 100 -> BudgetHealthStatus.OVER_BUDGET
            allocations.overallPercentageUsed >= 80 -> BudgetHealthStatus.WARNING
            else -> BudgetHealthStatus.HEALTHY
        }

        return BudgetHealthResponse(
            budgetId = budgetId.toString(),
            month = yearMonth.toString(),
            totalAllocated = allocations.totalAllocated,
            totalSpent = allocations.totalSpent,
            healthPercentage = allocations.overallPercentageUsed,
            status = status
        )
    }

    /**
     * Validate that user has access to budget.
     */
    private fun validateBudgetAccess(budgetId: UUID, userId: UUID) {
        val budget = budgetRepository.findById(budgetId)
            ?: throw EntityNotFoundException("Budget", budgetId)

        val user = userRepository.findById(userId)
            ?: throw EntityNotFoundException("User", userId)

        if (!budget.hasMember(user) && !budget.isOwner(user)) {
            logger.warn("Access denied for user $userId to budget $budgetId")
            throw AccessDeniedException("User does not have access to this budget", "Budget", budgetId)
        }
    }
}
