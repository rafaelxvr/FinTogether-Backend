package com.fintogether.presentation.controller

import com.fintogether.application.dto.*
import com.fintogether.application.service.BudgetAllocationService
import com.fintogether.infrastructure.security.UserPrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.YearMonth
import java.util.UUID

/**
 * Budget Allocation Controller
 * REST endpoints for managing budget allocations per category.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Budget Allocations", description = "Budget allocation per category management endpoints")
@SecurityRequirement(name = "bearerAuth")
class BudgetAllocationController(
    private val budgetAllocationService: BudgetAllocationService
) {

    @GetMapping("/budgets/{budgetId}/allocations")
    @Operation(summary = "Get budget allocations with actual spending for a month")
    fun getAllocations(
        @PathVariable budgetId: UUID,
        @Parameter(description = "Month in yyyy-MM format (e.g. 2026-03)")
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM") month: YearMonth,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<ApiResponse<BudgetAllocationsResponse>> {
        val result = budgetAllocationService.getAllocations(budgetId, month, userPrincipal.id)
        return ResponseEntity.ok(ApiResponse(success = true, data = result))
    }

    @PutMapping("/budgets/{budgetId}/allocations")
    @Operation(summary = "Set or update budget allocations for multiple categories")
    fun setAllocations(
        @PathVariable budgetId: UUID,
        @Valid @RequestBody request: SetAllocationsRequest,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<ApiResponse<BudgetAllocationsResponse>> {
        val result = budgetAllocationService.setAllocations(budgetId, request, userPrincipal.id)
        return ResponseEntity.ok(ApiResponse(success = true, data = result, message = "Allocations updated successfully"))
    }

    @GetMapping("/budgets/{budgetId}/budget-health")
    @Operation(summary = "Get budget health summary for a month")
    fun getBudgetHealth(
        @PathVariable budgetId: UUID,
        @Parameter(description = "Month in yyyy-MM format (e.g. 2026-03)")
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM") month: YearMonth,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<ApiResponse<BudgetHealthResponse>> {
        val result = budgetAllocationService.getBudgetHealth(budgetId, month, userPrincipal.id)
        return ResponseEntity.ok(ApiResponse(success = true, data = result))
    }
}
