package com.fintogether.domain.repository

import com.fintogether.domain.entity.Budget
import com.fintogether.domain.entity.CreditCard
import com.fintogether.domain.entity.InstallmentGroup
import java.util.UUID

interface InstallmentGroupRepository {
    fun save(installmentGroup: InstallmentGroup): InstallmentGroup
    fun findById(id: UUID): InstallmentGroup?
    fun findByBudget(budget: Budget): List<InstallmentGroup>
    fun findByBudgetId(budgetId: UUID): List<InstallmentGroup>
    fun findByCard(card: CreditCard): List<InstallmentGroup>
    fun delete(installmentGroup: InstallmentGroup)
}
