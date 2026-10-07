package com.fintogether.application.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDateTime
import java.util.UUID

data class CreateMerchantRequest(
    @field:NotBlank(message = "Merchant name is required")
    @field:Size(min = 2, max = 255, message = "Merchant name must be between 2 and 255 characters")
    val name: String,

    val displayName: String? = null,

    val logoUrl: String? = null,

    val color: String? = null,

    val defaultCategoryId: UUID? = null
)

data class UpdateMerchantRequest(
    @field:NotBlank(message = "Merchant name is required")
    @field:Size(min = 2, max = 255, message = "Merchant name must be between 2 and 255 characters")
    val name: String,

    val displayName: String? = null,

    val logoUrl: String? = null,

    val color: String? = null,

    val defaultCategoryId: UUID? = null
)

data class MerchantResponse(
    val id: UUID,
    val budgetId: UUID,
    val name: String,
    val displayName: String?,
    val logoUrl: String?,
    val color: String?,
    val defaultCategoryId: UUID?,
    val archived: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
