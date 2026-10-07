package com.fintogether.infrastructure.persistence

import com.fintogether.domain.entity.BudgetAllocation
import com.fintogether.domain.repository.BudgetAllocationRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.UUID

interface JpaBudgetAllocationRepositoryInterface : JpaRepository<BudgetAllocation, UUID> {
    fun findByBudgetIdAndMonth(budgetId: UUID, month: LocalDate): List<BudgetAllocation>
    fun findByBudgetIdAndCategoryIdAndMonth(budgetId: UUID, categoryId: UUID, month: LocalDate): BudgetAllocation?
    fun deleteByBudgetIdAndMonth(budgetId: UUID, month: LocalDate)
}

@Repository
class JpaBudgetAllocationRepository(
    private val jpaRepository: JpaBudgetAllocationRepositoryInterface
) : BudgetAllocationRepository {

    override fun save(allocation: BudgetAllocation): BudgetAllocation =
        jpaRepository.save(allocation)

    override fun saveAll(allocations: List<BudgetAllocation>): List<BudgetAllocation> =
        jpaRepository.saveAll(allocations)

    override fun findByBudgetIdAndMonth(budgetId: UUID, month: LocalDate): List<BudgetAllocation> =
        jpaRepository.findByBudgetIdAndMonth(budgetId, month)

    override fun findByBudgetIdAndCategoryIdAndMonth(budgetId: UUID, categoryId: UUID, month: LocalDate): BudgetAllocation? =
        jpaRepository.findByBudgetIdAndCategoryIdAndMonth(budgetId, categoryId, month)

    override fun deleteByBudgetIdAndMonth(budgetId: UUID, month: LocalDate) =
        jpaRepository.deleteByBudgetIdAndMonth(budgetId, month)
}
