package com.fintogether.infrastructure.persistence

import com.fintogether.domain.entity.SavingsGoalContribution
import com.fintogether.domain.repository.SavingsGoalContributionRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

interface JpaSavingsGoalContributionRepositoryInterface : JpaRepository<SavingsGoalContribution, UUID> {
    fun findBySavingsGoalId(savingsGoalId: UUID): List<SavingsGoalContribution>
    fun findBySavingsGoalIdOrderByContributedAtDesc(savingsGoalId: UUID, pageable: Pageable): Page<SavingsGoalContribution>
}

@Repository
class JpaSavingsGoalContributionRepository(
    private val jpaRepository: JpaSavingsGoalContributionRepositoryInterface
) : SavingsGoalContributionRepository {

    override fun save(contribution: SavingsGoalContribution): SavingsGoalContribution =
        jpaRepository.save(contribution)

    override fun findBySavingsGoalId(savingsGoalId: UUID): List<SavingsGoalContribution> =
        jpaRepository.findBySavingsGoalId(savingsGoalId)

    override fun findBySavingsGoalId(savingsGoalId: UUID, pageable: Pageable): Page<SavingsGoalContribution> =
        jpaRepository.findBySavingsGoalIdOrderByContributedAtDesc(savingsGoalId, pageable)
}
