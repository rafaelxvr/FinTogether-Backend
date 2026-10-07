package com.fintogether.application.mapper

import com.fintogether.application.dto.MerchantResponse
import com.fintogether.domain.entity.Merchant
import org.springframework.stereotype.Component

@Component
class MerchantMapper {

    fun toResponse(merchant: Merchant): MerchantResponse {
        return MerchantResponse(
            id = merchant.id,
            budgetId = merchant.budget.id,
            name = merchant.name,
            displayName = merchant.displayName,
            logoUrl = merchant.logoUrl,
            color = merchant.color,
            defaultCategoryId = merchant.defaultCategory?.id,
            archived = merchant.archived,
            createdAt = merchant.createdAt,
            updatedAt = merchant.updatedAt
        )
    }
}
