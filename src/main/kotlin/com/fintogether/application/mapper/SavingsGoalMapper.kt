package com.fintogether.application.mapper

import com.fintogether.application.dto.ContributionResponse
import com.fintogether.application.dto.SavingsGoalResponse
import com.fintogether.domain.entity.SavingsGoal
import com.fintogether.domain.entity.SavingsGoalContribution
import org.springframework.stereotype.Component

@Component
class SavingsGoalMapper {

    fun toResponse(savingsGoal: SavingsGoal): SavingsGoalResponse {
        return SavingsGoalResponse(
            id = savingsGoal.id,
            budgetId = savingsGoal.budget.id,
            accountId = savingsGoal.account?.id,
            name = savingsGoal.name,
            description = savingsGoal.description,
            targetAmount = savingsGoal.targetAmount,
            currentAmount = savingsGoal.currentAmount,
            currency = savingsGoal.currency,
            deadline = savingsGoal.deadline,
            icon = savingsGoal.icon,
            color = savingsGoal.color,
            active = savingsGoal.active,
            progressPercentage = savingsGoal.progressPercentage(),
            isCompleted = savingsGoal.isCompleted(),
            remainingAmount = savingsGoal.remainingAmount(),
            daysRemaining = savingsGoal.daysRemaining(),
            completedAt = savingsGoal.completedAt,
            createdAt = savingsGoal.createdAt,
            updatedAt = savingsGoal.updatedAt
        )
    }

    fun toContributionResponse(contribution: SavingsGoalContribution): ContributionResponse {
        return ContributionResponse(
            id = contribution.id,
            savingsGoalId = contribution.savingsGoal.id,
            amount = contribution.amount,
            note = contribution.note,
            contributedAt = contribution.contributedAt
        )
    }
}
