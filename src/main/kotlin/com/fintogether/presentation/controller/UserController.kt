package com.fintogether.presentation.controller

import com.fintogether.application.dto.*
import com.fintogether.application.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/users")
@Tag(name = "User Profile", description = "User profile management endpoints")
@SecurityRequirement(name = "bearerAuth")
class UserController(
    private val userService: UserService
) {

    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    fun getCurrentUser(): ResponseEntity<ApiResponse<UserResponse>> {
        val user = userService.getCurrentUserProfile()
        return ResponseEntity.ok(ApiResponse(success = true, data = user))
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user profile")
    fun updateProfile(
        @Valid @RequestBody request: UpdateProfileRequest
    ): ResponseEntity<ApiResponse<UserResponse>> {
        val user = userService.updateProfile(request)
        return ResponseEntity.ok(ApiResponse(success = true, data = user, message = "Profile updated successfully"))
    }

    @PutMapping("/me/password")
    @Operation(summary = "Change current user password")
    fun changePassword(
        @Valid @RequestBody request: ChangePasswordRequest
    ): ResponseEntity<ApiResponse<Nothing>> {
        userService.changePassword(request)
        return ResponseEntity.ok(ApiResponse(success = true, message = "Password changed successfully"))
    }

    @DeleteMapping("/me")
    @Operation(summary = "Delete current user account")
    fun deleteAccount(): ResponseEntity<ApiResponse<Nothing>> {
        userService.deleteAccount()
        return ResponseEntity.ok(ApiResponse(success = true, message = "Account deleted successfully"))
    }
}
