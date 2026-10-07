package com.fintogether.domain.repository

import com.fintogether.domain.entity.SavingsGoalContribution
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

interface SavingsGoalContributionRepository {
    fun save(contribution: SavingsGoalContribution): SavingsGoalContribution
    fun findBySavingsGoalId(savingsGoalId: UUID): List<SavingsGoalContribution>
    fun findBySavingsGoalId(savingsGoalId: UUID, pageable: Pageable): Page<SavingsGoalContribution>
}
