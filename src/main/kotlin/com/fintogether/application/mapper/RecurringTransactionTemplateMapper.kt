package com.fintogether.application.mapper

import com.fintogether.application.dto.RecurringTemplateResponse
import com.fintogether.domain.entity.RecurringTransactionTemplate
import org.springframework.stereotype.Component

@Component
class RecurringTransactionTemplateMapper {

    fun toResponse(template: RecurringTransactionTemplate): RecurringTemplateResponse {
        return RecurringTemplateResponse(
            id = template.id,
            budgetId = template.budget.id,
            userId = template.user.id,
            amount = template.amount,
            type = template.type,
            accountId = template.account?.id,
            categoryId = template.category?.id,
            cardId = template.card?.id,
            description = template.description,
            notes = template.notes,
            tags = template.tags,
            visibility = template.visibility,
            isFixedExpense = template.isFixedExpense,
            recurrenceRule = template.recurrenceRule,
            lastGeneratedDate = template.lastGeneratedDate,
            nextDueDate = template.nextDueDate,
            isActive = template.isActive,
            createdAt = template.createdAt,
            updatedAt = template.updatedAt
        )
    }
}
