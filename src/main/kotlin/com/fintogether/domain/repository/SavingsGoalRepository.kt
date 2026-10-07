package com.fintogether.domain.repository

import com.fintogether.domain.entity.SavingsGoal
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

interface SavingsGoalRepository {
    fun save(savingsGoal: SavingsGoal): SavingsGoal
    fun findById(id: UUID): SavingsGoal?
    fun findByBudgetId(budgetId: UUID): List<SavingsGoal>
    fun findByBudgetId(budgetId: UUID, pageable: Pageable): Page<SavingsGoal>
    fun findByBudgetIdAndActive(budgetId: UUID, active: Boolean): List<SavingsGoal>
    fun delete(savingsGoal: SavingsGoal)
}
