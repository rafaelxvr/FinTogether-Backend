package com.fintogether.application.mapper

import com.fintogether.application.dto.SplitResponse
import com.fintogether.domain.entity.TransactionSplit
import org.springframework.stereotype.Component

@Component
class TransactionSplitMapper {

    fun toResponse(split: TransactionSplit): SplitResponse {
        return SplitResponse(
            id = split.id,
            transactionId = split.transaction.id,
            userId = split.user.id,
            userName = split.user.name,
            amount = split.amount,
            isPaid = split.isPaid,
            paidAt = split.paidAt,
            createdAt = split.createdAt
        )
    }
}
