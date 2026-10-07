package com.fintogether.domain.repository

import com.fintogether.domain.entity.Budget
import com.fintogether.domain.entity.CreditCard
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

interface CreditCardRepository {
    fun save(creditCard: CreditCard): CreditCard
    fun findById(id: UUID): CreditCard?
    fun findByBudget(budget: Budget): List<CreditCard>
    fun findByBudgetId(budgetId: UUID): List<CreditCard>
    fun findByBudgetId(budgetId: UUID, pageable: Pageable): Page<CreditCard>
    fun findByBudgetAndActive(budget: Budget, active: Boolean): List<CreditCard>
    fun delete(creditCard: CreditCard)
}
