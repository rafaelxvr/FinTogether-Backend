package com.fintogether.infrastructure.persistence

import com.fintogether.domain.entity.Budget
import com.fintogether.domain.entity.CreditCard
import com.fintogether.domain.entity.InstallmentGroup
import com.fintogether.domain.repository.InstallmentGroupRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

interface JpaInstallmentGroupRepositoryInterface : JpaRepository<InstallmentGroup, UUID> {
    fun findByBudget(budget: Budget): List<InstallmentGroup>
    fun findByBudgetId(budgetId: UUID): List<InstallmentGroup>
    fun findByCard(card: CreditCard): List<InstallmentGroup>
}

@Repository
class JpaInstallmentGroupRepository(
    private val jpaRepository: JpaInstallmentGroupRepositoryInterface
) : InstallmentGroupRepository {

    override fun save(group: InstallmentGroup): InstallmentGroup = jpaRepository.save(group)

    override fun findById(id: UUID): InstallmentGroup? = jpaRepository.findById(id).orElse(null)

    override fun findByBudget(budget: Budget): List<InstallmentGroup> = jpaRepository.findByBudget(budget)

    override fun findByBudgetId(budgetId: UUID): List<InstallmentGroup> = jpaRepository.findByBudgetId(budgetId)

    override fun findByCard(card: CreditCard): List<InstallmentGroup> = jpaRepository.findByCard(card)

    override fun delete(group: InstallmentGroup) = jpaRepository.delete(group)
}
