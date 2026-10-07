package com.fintogether.presentation.controller

import com.fintogether.application.dto.*
import com.fintogether.application.service.SavingsGoalService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api")
@Tag(name = "Savings Goals", description = "Savings goal management endpoints")
@SecurityRequirement(name = "bearerAuth")
class SavingsGoalController(
    private val savingsGoalService: SavingsGoalService
) {

    @PostMapping("/budgets/{budgetId}/savings-goals")
    @Operation(summary = "Create a new savings goal in a budget")
    fun createSavingsGoal(
        @PathVariable budgetId: UUID,
        @Valid @RequestBody request: CreateSavingsGoalRequest
    ): ResponseEntity<ApiResponse<SavingsGoalResponse>> {
        val goal = savingsGoalService.createSavingsGoal(budgetId, request)
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse(success = true, data = goal, message = "Savings goal created successfully"))
    }

    @GetMapping("/budgets/{budgetId}/savings-goals")
    @Operation(summary = "Get all savings goals for a budget. Supports pagination.")
    fun getSavingsGoalsByBudget(
        @PathVariable budgetId: UUID,
        @Parameter(description = "Page number (0-based)")
        @RequestParam(required = false) page: Int?,
        @Parameter(description = "Page size (default 20)")
        @RequestParam(required = false, defaultValue = "20") size: Int
    ): ResponseEntity<*> {
        return if (page != null) {
            val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"))
            val pagedResult = savingsGoalService.getSavingsGoalsByBudget(budgetId, pageable)
            ResponseEntity.ok(ApiResponse(success = true, data = pagedResult))
        } else {
            val goals = savingsGoalService.getSavingsGoalsByBudget(budgetId)
            ResponseEntity.ok(ApiResponse(success = true, data = goals))
        }
    }

    @GetMapping("/savings-goals/{id}")
    @Operation(summary = "Get savings goal by ID")
    fun getSavingsGoalById(@PathVariable id: UUID): ResponseEntity<ApiResponse<SavingsGoalResponse>> {
        val goal = savingsGoalService.getSavingsGoalById(id)
        return ResponseEntity.ok(ApiResponse(success = true, data = goal))
    }

    @PutMapping("/savings-goals/{id}")
    @Operation(summary = "Update savings goal")
    fun updateSavingsGoal(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateSavingsGoalRequest
    ): ResponseEntity<ApiResponse<SavingsGoalResponse>> {
        val goal = savingsGoalService.updateSavingsGoal(id, request)
        return ResponseEntity.ok(ApiResponse(success = true, data = goal, message = "Savings goal updated successfully"))
    }

    @DeleteMapping("/savings-goals/{id}")
    @Operation(summary = "Delete savings goal")
    fun deleteSavingsGoal(@PathVariable id: UUID): ResponseEntity<ApiResponse<Nothing>> {
        savingsGoalService.deleteSavingsGoal(id)
        return ResponseEntity.ok(ApiResponse(success = true, message = "Savings goal deleted successfully"))
    }

    @PostMapping("/savings-goals/{id}/contribute")
    @Operation(summary = "Contribute to a savings goal")
    fun contribute(
        @PathVariable id: UUID,
        @Valid @RequestBody request: ContributeRequest
    ): ResponseEntity<ApiResponse<SavingsGoalResponse>> {
        val goal = savingsGoalService.contribute(id, request)
        return ResponseEntity.ok(ApiResponse(success = true, data = goal, message = "Contribution recorded successfully"))
    }

    @PostMapping("/savings-goals/{id}/withdraw")
    @Operation(summary = "Withdraw from a savings goal")
    fun withdraw(
        @PathVariable id: UUID,
        @Valid @RequestBody request: WithdrawRequest
    ): ResponseEntity<ApiResponse<SavingsGoalResponse>> {
        val goal = savingsGoalService.withdraw(id, request)
        return ResponseEntity.ok(ApiResponse(success = true, data = goal, message = "Withdrawal recorded successfully"))
    }

    @GetMapping("/savings-goals/{id}/contributions")
    @Operation(summary = "Get contribution history for a savings goal")
    fun getContributions(@PathVariable id: UUID): ResponseEntity<ApiResponse<List<ContributionResponse>>> {
        val contributions = savingsGoalService.getContributions(id)
        return ResponseEntity.ok(ApiResponse(success = true, data = contributions))
    }
}
