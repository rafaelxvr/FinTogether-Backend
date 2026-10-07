package com.fintogether.domain.repository

import com.fintogether.domain.entity.Account
import com.fintogether.domain.entity.Budget
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

interface AccountRepository {
    fun save(account: Account): Account
    fun findById(id: UUID): Account?
    fun findByBudget(budget: Budget): List<Account>
    fun findByBudgetId(budgetId: UUID): List<Account>
    fun findByBudgetId(budgetId: UUID, pageable: Pageable): Page<Account>
    fun findByBudgetAndActive(budget: Budget, active: Boolean): List<Account>
    fun delete(account: Account)
}
