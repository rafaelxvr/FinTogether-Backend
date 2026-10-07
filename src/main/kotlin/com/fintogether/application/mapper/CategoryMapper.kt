package com.fintogether.application.mapper

import com.fintogether.application.dto.CategoryResponse
import com.fintogether.domain.entity.Category
import org.springframework.stereotype.Component

@Component
class CategoryMapper {

    fun toResponse(category: Category): CategoryResponse {
        return CategoryResponse(
            id = category.id,
            budgetId = category.budget.id,
            name = category.name,
            type = category.type,
            parentId = category.parent?.id,
            icon = category.icon,
            color = category.color,
            limitAmount = category.limitAmount,
            archived = category.archived,
            isEssential = category.isEssential,
            createdAt = category.createdAt,
            hasChildren = category.children.isNotEmpty()
        )
    }
}
