package com.fintogether.application.mapper

import com.fintogether.application.dto.AccountResponse
import com.fintogether.domain.entity.Account
import org.springframework.stereotype.Component

@Component
class AccountMapper {

    fun toResponse(account: Account): AccountResponse {
        return AccountResponse(
            id = account.id,
            budgetId = account.budget.id,
            name = account.name,
            type = account.type,
            currency = account.currency,
            openingBalance = account.openingBalance,
            currentBalance = account.currentBalance,
            active = account.active,
            createdAt = account.createdAt
        )
    }
}
