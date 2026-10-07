package com.fintogether.infrastructure.persistence

import com.fintogether.domain.entity.Account
import com.fintogether.domain.entity.Budget
import com.fintogether.domain.repository.AccountRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

interface JpaAccountRepositoryInterface : JpaRepository<Account, UUID> {
    fun findByBudget(budget: Budget): List<Account>
    fun findByBudgetId(budgetId: UUID): List<Account>
    fun findByBudgetId(budgetId: UUID, pageable: Pageable): Page<Account>
    fun findByBudgetAndActive(budget: Budget, active: Boolean): List<Account>
}

@Repository
class JpaAccountRepository(
    private val jpaRepository: JpaAccountRepositoryInterface
) : AccountRepository {

    override fun save(account: Account): Account = jpaRepository.save(account)

    override fun findById(id: UUID): Account? = jpaRepository.findById(id).orElse(null)

    override fun findByBudget(budget: Budget): List<Account> = jpaRepository.findByBudget(budget)

    override fun findByBudgetId(budgetId: UUID): List<Account> = jpaRepository.findByBudgetId(budgetId)

    override fun findByBudgetId(budgetId: UUID, pageable: Pageable): Page<Account> =
        jpaRepository.findByBudgetId(budgetId, pageable)

    override fun findByBudgetAndActive(budget: Budget, active: Boolean): List<Account> =
        jpaRepository.findByBudgetAndActive(budget, active)

    override fun delete(account: Account) = jpaRepository.delete(account)
}
