package com.fintogether.application.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class UpdateProfileRequest(
    @field:NotBlank(message = "Name is required")
    @field:Size(min = 2, max = 255, message = "Name must be between 2 and 255 characters")
    val name: String,

    val locale: String? = null,

    val settings: Map<String, Any>? = null
)

data class ChangePasswordRequest(
    @field:NotBlank(message = "Current password is required")
    val currentPassword: String,

    @field:NotBlank(message = "New password is required")
    @field:Size(min = 6, message = "Password must be at least 6 characters")
    val newPassword: String
)
