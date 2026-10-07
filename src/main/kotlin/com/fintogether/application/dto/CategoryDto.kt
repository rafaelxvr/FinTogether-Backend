package com.fintogether.application.dto

import com.fintogether.domain.entity.CategoryType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

data class CreateCategoryRequest(
    @field:NotBlank(message = "Category name is required")
    @field:Size(min = 2, max = 255, message = "Category name must be between 2 and 255 characters")
    val name: String,

    @field:NotNull(message = "Category type is required")
    val type: CategoryType,

    val parentId: UUID? = null,

    val icon: String? = null,

    val color: String? = null,

    val limitAmount: BigDecimal? = null,

    val isEssential: Boolean = false
)

data class UpdateCategoryRequest(
    @field:NotBlank(message = "Category name is required")
    @field:Size(min = 2, max = 255, message = "Category name must be between 2 and 255 characters")
    val name: String,

    val icon: String? = null,

    val color: String? = null,

    val limitAmount: BigDecimal? = null,

    val parentId: UUID? = null,

    val isEssential: Boolean = false
)

data class CategoryResponse(
    val id: UUID,
    val budgetId: UUID,
    val name: String,
    val type: CategoryType,
    val parentId: UUID?,
    val icon: String?,
    val color: String?,
    val limitAmount: BigDecimal?,
    val archived: Boolean,
    val isEssential: Boolean,
    val createdAt: LocalDateTime,
    val hasChildren: Boolean
)
