package com.fintogether.infrastructure.persistence

import com.fintogether.domain.entity.SavingsGoal
import com.fintogether.domain.repository.SavingsGoalRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

interface JpaSavingsGoalRepositoryInterface : JpaRepository<SavingsGoal, UUID> {
    fun findByBudgetId(budgetId: UUID): List<SavingsGoal>
    fun findByBudgetId(budgetId: UUID, pageable: Pageable): Page<SavingsGoal>
    fun findByBudgetIdAndActive(budgetId: UUID, active: Boolean): List<SavingsGoal>
}

@Repository
class JpaSavingsGoalRepository(
    private val jpaRepository: JpaSavingsGoalRepositoryInterface
) : SavingsGoalRepository {

    override fun save(savingsGoal: SavingsGoal): SavingsGoal = jpaRepository.save(savingsGoal)

    override fun findById(id: UUID): SavingsGoal? = jpaRepository.findById(id).orElse(null)

    override fun findByBudgetId(budgetId: UUID): List<SavingsGoal> =
        jpaRepository.findByBudgetId(budgetId)

    override fun findByBudgetId(budgetId: UUID, pageable: Pageable): Page<SavingsGoal> =
        jpaRepository.findByBudgetId(budgetId, pageable)

    override fun findByBudgetIdAndActive(budgetId: UUID, active: Boolean): List<SavingsGoal> =
        jpaRepository.findByBudgetIdAndActive(budgetId, active)

    override fun delete(savingsGoal: SavingsGoal) = jpaRepository.delete(savingsGoal)
}
