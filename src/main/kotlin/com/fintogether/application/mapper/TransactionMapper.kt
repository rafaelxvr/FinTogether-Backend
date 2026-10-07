package com.fintogether.application.mapper

import com.fintogether.application.dto.TransactionResponse
import com.fintogether.domain.entity.Transaction
import org.springframework.stereotype.Component

@Component
class TransactionMapper {

    fun toResponse(transaction: Transaction): TransactionResponse {
        return TransactionResponse(
            id = transaction.id,
            budgetId = transaction.budget.id,
            userId = transaction.user?.id,
            transactionDate = transaction.transactionDate,
            amount = transaction.amount,
            type = transaction.type,
            accountId = transaction.account?.id,
            categoryId = transaction.category?.id,
            cardId = transaction.card?.id,
            description = transaction.description,
            notes = transaction.notes,
            tags = transaction.tags,
            visibility = transaction.visibility,
            isRecurring = transaction.isRecurring,
            recurrenceRule = transaction.recurrenceRule,
            isInstallment = transaction.isInstallment,
            installmentGroupId = transaction.installmentGroupId,
            installmentNumber = transaction.installmentNumber,
            parentRecurringId = transaction.parentRecurringId,
            merchantId = transaction.merchant?.id,
            syncedAt = transaction.syncedAt,
            createdAt = transaction.createdAt,
            updatedAt = transaction.updatedAt
        )
    }
}
