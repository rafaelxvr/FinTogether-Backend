package com.fintogether.domain.repository

import com.fintogether.domain.entity.Budget
import com.fintogether.domain.entity.Category
import com.fintogether.domain.entity.CreditCard
import com.fintogether.domain.entity.Transaction
import com.fintogether.domain.entity.TransactionType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

interface TransactionRepository {
    fun save(transaction: Transaction): Transaction
    fun saveAll(transactions: List<Transaction>): List<Transaction>
    fun findById(id: UUID): Transaction?
    fun findByBudget(budget: Budget): List<Transaction>
    fun findByBudgetId(budgetId: UUID): List<Transaction>
    fun findByBudgetId(budgetId: UUID, pageable: Pageable): Page<Transaction>
    fun findByBudgetAndDateBetween(budget: Budget, startDate: LocalDate, endDate: LocalDate): List<Transaction>
    fun findByBudgetIdAndDateBetween(budgetId: UUID, startDate: LocalDate, endDate: LocalDate): List<Transaction>
    fun findByBudgetIdAndDateBetween(budgetId: UUID, startDate: LocalDate, endDate: LocalDate, pageable: Pageable): Page<Transaction>
    fun findByCategory(category: Category): List<Transaction>
    fun findByCard(card: CreditCard): List<Transaction>
    fun findByCardAndDateBetween(card: CreditCard, startDate: LocalDate, endDate: LocalDate): List<Transaction>
    fun findByInstallmentGroupId(groupId: UUID): List<Transaction>
    fun findByParentRecurringId(parentRecurringId: UUID): List<Transaction>
    fun countByParentRecurringId(parentRecurringId: UUID): Long
    fun search(
        budgetId: UUID,
        query: String?,
        type: TransactionType?,
        categoryId: UUID?,
        merchantId: UUID?,
        minAmount: BigDecimal?,
        maxAmount: BigDecimal?,
        startDate: LocalDate?,
        endDate: LocalDate?,
        pageable: Pageable
    ): Page<Transaction>
    fun delete(transaction: Transaction)
    fun deleteAll(transactions: List<Transaction>)
}
