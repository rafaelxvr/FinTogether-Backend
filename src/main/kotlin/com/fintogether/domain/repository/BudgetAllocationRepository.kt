package com.fintogether.domain.repository

import com.fintogether.domain.entity.BudgetAllocation
import java.time.LocalDate
import java.util.UUID

interface BudgetAllocationRepository {
    fun save(allocation: BudgetAllocation): BudgetAllocation
    fun saveAll(allocations: List<BudgetAllocation>): List<BudgetAllocation>
    fun findByBudgetIdAndMonth(budgetId: UUID, month: LocalDate): List<BudgetAllocation>
    fun findByBudgetIdAndCategoryIdAndMonth(budgetId: UUID, categoryId: UUID, month: LocalDate): BudgetAllocation?
    fun deleteByBudgetIdAndMonth(budgetId: UUID, month: LocalDate)
}
