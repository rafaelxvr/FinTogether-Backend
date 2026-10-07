package com.fintogether.presentation.controller

import com.fintogether.application.dto.*
import com.fintogether.application.service.AuthService
import com.fintogether.infrastructure.security.UserPrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication endpoints")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<ApiResponse<AuthResponse>> {
        val response = authService.register(request)
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse(success = true, data = response, message = "User registered successfully"))
    }

    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<ApiResponse<AuthResponse>> {
        val response = authService.login(request)
        return ResponseEntity.ok(ApiResponse(success = true, data = response, message = "Login successful"))
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token")
    fun refreshToken(@Valid @RequestBody request: RefreshTokenRequest): ResponseEntity<ApiResponse<AuthResponse>> {
        val response = authService.refreshToken(request)
        return ResponseEntity.ok(ApiResponse(success = true, data = response, message = "Token refreshed successfully"))
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user information")
    fun getCurrentUser(@AuthenticationPrincipal userPrincipal: UserPrincipal): ResponseEntity<ApiResponse<UserResponse>> {
        val response = UserResponse(
            id = userPrincipal.id,
            name = userPrincipal.username,
            email = userPrincipal.username,
            locale = "pt-BR" // You may want to fetch this from the database
        )
        return ResponseEntity.ok(ApiResponse(success = true, data = response))
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset")
    fun forgotPassword(@Valid @RequestBody request: ForgotPasswordRequest): ResponseEntity<ApiResponse<String>> {
        authService.requestPasswordReset(request)
        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                data = "If an account with this email exists, a password reset link will be sent.",
                message = "Password reset requested"
            )
        )
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password with token")
    fun resetPassword(@Valid @RequestBody request: ResetPasswordRequest): ResponseEntity<ApiResponse<String>> {
        authService.resetPassword(request)
        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                data = "Password has been reset successfully",
                message = "Password reset successful"
            )
        )
    }
}
