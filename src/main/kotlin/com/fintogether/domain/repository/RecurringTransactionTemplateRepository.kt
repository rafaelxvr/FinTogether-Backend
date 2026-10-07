package com.fintogether.domain.repository

import com.fintogether.domain.entity.RecurringTransactionTemplate
import java.time.LocalDate
import java.util.UUID

interface RecurringTransactionTemplateRepository {
    fun save(template: RecurringTransactionTemplate): RecurringTransactionTemplate
    fun findById(id: UUID): RecurringTransactionTemplate?
    fun findByBudgetId(budgetId: UUID): List<RecurringTransactionTemplate>
    fun findByBudgetIdAndIsActive(budgetId: UUID, isActive: Boolean): List<RecurringTransactionTemplate>
    fun findByBudgetIdAndIsFixedExpense(budgetId: UUID, isFixedExpense: Boolean): List<RecurringTransactionTemplate>
    fun findByIsActiveAndNextDueDateLessThanEqual(isActive: Boolean, date: LocalDate): List<RecurringTransactionTemplate>
    fun delete(template: RecurringTransactionTemplate)
}
