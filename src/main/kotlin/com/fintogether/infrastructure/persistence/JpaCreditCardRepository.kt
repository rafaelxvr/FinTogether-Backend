package com.fintogether.infrastructure.persistence

import com.fintogether.domain.entity.Budget
import com.fintogether.domain.entity.CreditCard
import com.fintogether.domain.repository.CreditCardRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

interface JpaCreditCardRepositoryInterface : JpaRepository<CreditCard, UUID> {
    fun findByBudget(budget: Budget): List<CreditCard>
    fun findByBudgetId(budgetId: UUID): List<CreditCard>
    fun findByBudgetId(budgetId: UUID, pageable: Pageable): Page<CreditCard>
    fun findByBudgetAndActive(budget: Budget, active: Boolean): List<CreditCard>
}

@Repository
class JpaCreditCardRepository(
    private val jpaRepository: JpaCreditCardRepositoryInterface
) : CreditCardRepository {

    override fun save(creditCard: CreditCard): CreditCard = jpaRepository.save(creditCard)

    override fun findById(id: UUID): CreditCard? = jpaRepository.findById(id).orElse(null)

    override fun findByBudget(budget: Budget): List<CreditCard> = jpaRepository.findByBudget(budget)

    override fun findByBudgetId(budgetId: UUID): List<CreditCard> = jpaRepository.findByBudgetId(budgetId)

    override fun findByBudgetId(budgetId: UUID, pageable: Pageable): Page<CreditCard> =
        jpaRepository.findByBudgetId(budgetId, pageable)

    override fun findByBudgetAndActive(budget: Budget, active: Boolean): List<CreditCard> =
        jpaRepository.findByBudgetAndActive(budget, active)

    override fun delete(creditCard: CreditCard) = jpaRepository.delete(creditCard)
}
