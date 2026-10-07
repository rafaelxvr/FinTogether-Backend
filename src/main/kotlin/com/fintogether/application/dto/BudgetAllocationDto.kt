package com.fintogether.application.dto

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import org.springframework.format.annotation.DateTimeFormat
import java.math.BigDecimal
import java.time.YearMonth
import java.util.UUID

/**
 * Request to set or update allocations for a budget and month.
 */
data class SetAllocationsRequest(
    @field:NotNull
    @field:DateTimeFormat(pattern = "yyyy-MM")
    val month: YearMonth,

    @field:NotEmpty
    val allocations: List<CategoryAllocationInput>
)

/**
 * Input for a single category allocation.
 */
data class CategoryAllocationInput(
    @field:NotNull
    val categoryId: UUID,

    @field:NotNull
    @field:DecimalMin("0")
    val allocatedAmount: BigDecimal
)

/**
 * Response containing all allocations for a budget and month, with actual spending data.
 */
data class BudgetAllocationsResponse(
    val budgetId: String,
    val month: String,
    val totalAllocated: BigDecimal,
    val totalSpent: BigDecimal,
    val totalRemaining: BigDecimal,
    val overallPercentageUsed: Double,
    val categories: List<CategoryAllocationResponse>
)

/**
 * Response for a single category's allocation with spending data.
 */
data class CategoryAllocationResponse(
    val categoryId: String,
    val categoryName: String,
    val categoryIcon: String?,
    val categoryColor: String?,
    val allocatedAmount: BigDecimal,
    val spentAmount: BigDecimal,
    val remainingAmount: BigDecimal,
    val percentageUsed: Double,
    val limitAmount: BigDecimal?,
    val isOverBudget: Boolean
)

/**
 * Budget health summary response.
 */
data class BudgetHealthResponse(
    val budgetId: String,
    val month: String,
    val totalAllocated: BigDecimal,
    val totalSpent: BigDecimal,
    val healthPercentage: Double,
    val status: BudgetHealthStatus
)

/**
 * Budget health status based on spending percentage.
 */
enum class BudgetHealthStatus {
    HEALTHY,    // < 80%
    WARNING,    // 80-100%
    OVER_BUDGET // > 100%
}
