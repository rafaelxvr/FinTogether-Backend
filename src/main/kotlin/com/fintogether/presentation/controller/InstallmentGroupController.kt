package com.fintogether.presentation.controller

import com.fintogether.application.dto.ApiResponse
import com.fintogether.application.dto.CreateInstallmentGroupRequest
import com.fintogether.application.dto.InstallmentGroupProgressResponse
import com.fintogether.application.dto.InstallmentGroupResponse
import com.fintogether.application.service.InstallmentGroupService
import com.fintogether.infrastructure.security.UserPrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api")
@Tag(name = "Installment Groups", description = "Installment group management endpoints")
@SecurityRequirement(name = "bearerAuth")
class InstallmentGroupController(
    private val installmentGroupService: InstallmentGroupService
) {

    @PostMapping("/budgets/{budgetId}/installment-groups")
    @Operation(summary = "Create a new installment group with auto-generated transactions")
    fun createInstallmentGroup(
        @PathVariable budgetId: UUID,
        @Valid @RequestBody request: CreateInstallmentGroupRequest
    ): ResponseEntity<ApiResponse<InstallmentGroupResponse>> {
        val userId = getCurrentUserId()
        val group = installmentGroupService.createInstallmentGroup(budgetId, request, userId)
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse(success = true, data = group, message = "Installment group created successfully"))
    }

    @GetMapping("/budgets/{budgetId}/installment-groups")
    @Operation(summary = "Get all installment groups for a budget")
    fun getInstallmentGroupsByBudget(
        @PathVariable budgetId: UUID
    ): ResponseEntity<ApiResponse<List<InstallmentGroupResponse>>> {
        val userId = getCurrentUserId()
        val groups = installmentGroupService.getInstallmentGroupsByBudget(budgetId, userId)
        return ResponseEntity.ok(ApiResponse(success = true, data = groups))
    }

    @GetMapping("/installment-groups/{groupId}")
    @Operation(summary = "Get installment group progress details")
    fun getInstallmentGroupProgress(
        @PathVariable groupId: UUID
    ): ResponseEntity<ApiResponse<InstallmentGroupProgressResponse>> {
        val userId = getCurrentUserId()
        val progress = installmentGroupService.getInstallmentGroupProgress(groupId, userId)
        return ResponseEntity.ok(ApiResponse(success = true, data = progress))
    }

    @DeleteMapping("/installment-groups/{groupId}")
    @Operation(summary = "Delete an installment group and its transactions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteInstallmentGroup(
        @PathVariable groupId: UUID
    ): ResponseEntity<Void> {
        val userId = getCurrentUserId()
        installmentGroupService.deleteInstallmentGroup(groupId, userId)
        return ResponseEntity.noContent().build()
    }

    private fun getCurrentUserId(): UUID {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as UserPrincipal
        return principal.id
    }
}
