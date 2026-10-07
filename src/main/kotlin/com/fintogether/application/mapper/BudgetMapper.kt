package com.fintogether.application.mapper

import com.fintogether.application.dto.BudgetMemberResponse
import com.fintogether.application.dto.BudgetResponse
import com.fintogether.domain.entity.Budget
import com.fintogether.domain.entity.BudgetMember
import org.springframework.stereotype.Component

@Component
class BudgetMapper {

    fun toResponse(budget: Budget): BudgetResponse {
        return BudgetResponse(
            id = budget.id,
            name = budget.name,
            type = budget.type,
            ownerId = budget.owner.id,
            ownerName = budget.owner.name,
            memberCount = budget.members.size,
            createdAt = budget.createdAt,
            updatedAt = budget.updatedAt
        )
    }

    fun toMemberResponse(member: BudgetMember): BudgetMemberResponse {
        return BudgetMemberResponse(
            id = member.id,
            budgetId = member.budget.id,
            userId = member.user.id,
            userName = member.user.name,
            userEmail = member.user.email,
            role = member.role.name,
            joinedAt = member.joinedAt
        )
    }
}
