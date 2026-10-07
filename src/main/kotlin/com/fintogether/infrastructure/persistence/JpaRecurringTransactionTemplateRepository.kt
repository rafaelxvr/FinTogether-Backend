package com.fintogether.infrastructure.persistence

import com.fintogether.domain.entity.RecurringTransactionTemplate
import com.fintogether.domain.repository.RecurringTransactionTemplateRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.UUID

interface JpaRecurringTransactionTemplateRepositoryInterface : JpaRepository<RecurringTransactionTemplate, UUID> {
    fun findByBudgetId(budgetId: UUID): List<RecurringTransactionTemplate>
    fun findByBudgetIdAndIsActive(budgetId: UUID, isActive: Boolean): List<RecurringTransactionTemplate>
    fun findByBudgetIdAndIsFixedExpense(budgetId: UUID, isFixedExpense: Boolean): List<RecurringTransactionTemplate>
    fun findByIsActiveAndNextDueDateLessThanEqual(isActive: Boolean, date: LocalDate): List<RecurringTransactionTemplate>
}

@Repository
class JpaRecurringTransactionTemplateRepository(
    private val jpaRepository: JpaRecurringTransactionTemplateRepositoryInterface
) : RecurringTransactionTemplateRepository {

    override fun save(template: RecurringTransactionTemplate): RecurringTransactionTemplate =
        jpaRepository.save(template)

    override fun findById(id: UUID): RecurringTransactionTemplate? =
        jpaRepository.findById(id).orElse(null)

    override fun findByBudgetId(budgetId: UUID): List<RecurringTransactionTemplate> =
        jpaRepository.findByBudgetId(budgetId)

    override fun findByBudgetIdAndIsActive(budgetId: UUID, isActive: Boolean): List<RecurringTransactionTemplate> =
        jpaRepository.findByBudgetIdAndIsActive(budgetId, isActive)

    override fun findByBudgetIdAndIsFixedExpense(budgetId: UUID, isFixedExpense: Boolean): List<RecurringTransactionTemplate> =
        jpaRepository.findByBudgetIdAndIsFixedExpense(budgetId, isFixedExpense)

    override fun findByIsActiveAndNextDueDateLessThanEqual(isActive: Boolean, date: LocalDate): List<RecurringTransactionTemplate> =
        jpaRepository.findByIsActiveAndNextDueDateLessThanEqual(isActive, date)

    override fun delete(template: RecurringTransactionTemplate) =
        jpaRepository.delete(template)
}
