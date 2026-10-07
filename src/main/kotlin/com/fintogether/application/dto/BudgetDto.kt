package com.fintogether.application.dto

import com.fintogether.domain.entity.BudgetType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDateTime
import java.util.UUID

data class CreateBudgetRequest(
    @field:NotBlank(message = "Budget name is required")
    @field:Size(min = 2, max = 255, message = "Budget name must be between 2 and 255 characters")
    val name: String,

    val type: BudgetType = BudgetType.PERSONAL,

    val locale: String? = null
)

data class UpdateBudgetRequest(
    @field:NotBlank(message = "Budget name is required")
    @field:Size(min = 2, max = 255, message = "Budget name must be between 2 and 255 characters")
    val name: String,

    val type: BudgetType
)

data class BudgetResponse(
    val id: UUID,
    val name: String,
    val type: BudgetType,
    val ownerId: UUID,
    val ownerName: String,
    val memberCount: Int,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class BudgetMemberRequest(
    @field:NotBlank(message = "User email is required")
    val email: String,

    val role: String = "MEMBER"
)

data class BudgetMemberResponse(
    val id: UUID,
    val budgetId: UUID,
    val userId: UUID,
    val userName: String,
    val userEmail: String,
    val role: String,
    val joinedAt: LocalDateTime
)
