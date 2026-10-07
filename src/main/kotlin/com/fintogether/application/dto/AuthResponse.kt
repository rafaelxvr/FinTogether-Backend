package com.fintogether.application.dto

import java.util.UUID

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val user: UserResponse
)

data class UserResponse(
    val id: UUID,
    val name: String,
    val email: String,
    val locale: String
)

data class ApiResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null
)
