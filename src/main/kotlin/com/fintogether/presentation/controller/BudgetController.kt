package com.fintogether.presentation.controller

import com.fintogether.application.dto.*
import com.fintogether.application.service.BudgetService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.UUID

@RestController
@RequestMapping("/api/budgets")
@Tag(name = "Budgets", description = "Budget management endpoints")
@SecurityRequirement(name = "bearerAuth")
class BudgetController(
    private val budgetService: BudgetService
) {

    @PostMapping
    @Operation(summary = "Create a new budget")
    fun createBudget(
        @Valid @RequestBody request: CreateBudgetRequest
    ): ResponseEntity<ApiResponse<BudgetResponse>> {
        val budget = budgetService.createBudget(request)
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse(success = true, data = budget, message = "Budget created successfully"))
    }

    @GetMapping
    @Operation(summary = "Get all budgets for current user. Supports pagination.")
    fun getAllBudgets(
        @Parameter(description = "Page number (0-based)")
        @RequestParam(required = false) page: Int?,
        @Parameter(description = "Page size (default 20)")
        @RequestParam(required = false, defaultValue = "20") size: Int,
        @Parameter(description = "Only return records updated after this ISO-8601 timestamp")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) updatedSince: Instant?
    ): ResponseEntity<*> {
        return if (page != null) {
            val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
            val pagedResult = budgetService.getAllBudgets(pageable)
            ResponseEntity.ok(ApiResponse(success = true, data = pagedResult))
        } else {
            val budgets = budgetService.getAllBudgets(updatedSince)
            ResponseEntity.ok(ApiResponse(success = true, data = budgets))
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get budget by ID")
    fun getBudgetById(@PathVariable id: UUID): ResponseEntity<ApiResponse<BudgetResponse>> {
        val budget = budgetService.getBudgetById(id)
        return ResponseEntity.ok(ApiResponse(success = true, data = budget))
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update budget")
    fun updateBudget(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateBudgetRequest
    ): ResponseEntity<ApiResponse<BudgetResponse>> {
        val budget = budgetService.updateBudget(id, request)
        return ResponseEntity.ok(ApiResponse(success = true, data = budget, message = "Budget updated successfully"))
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete budget")
    fun deleteBudget(@PathVariable id: UUID): ResponseEntity<ApiResponse<Nothing>> {
        budgetService.deleteBudget(id)
        return ResponseEntity.ok(ApiResponse(success = true, message = "Budget deleted successfully"))
    }

    @GetMapping("/{id}/members")
    @Operation(summary = "Get all members of a budget")
    fun getBudgetMembers(@PathVariable id: UUID): ResponseEntity<ApiResponse<List<BudgetMemberResponse>>> {
        val members = budgetService.getBudgetMembers(id)
        return ResponseEntity.ok(ApiResponse(success = true, data = members))
    }

    @PostMapping("/{id}/members")
    @Operation(summary = "Add a member to budget")
    fun addMember(
        @PathVariable id: UUID,
        @Valid @RequestBody request: BudgetMemberRequest
    ): ResponseEntity<ApiResponse<BudgetMemberResponse>> {
        val member = budgetService.addMember(id, request)
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse(success = true, data = member, message = "Member added successfully"))
    }

    @DeleteMapping("/{budgetId}/members/{memberId}")
    @Operation(summary = "Remove a member from budget")
    fun removeMember(
        @PathVariable budgetId: UUID,
        @PathVariable memberId: UUID
    ): ResponseEntity<ApiResponse<Nothing>> {
        budgetService.removeMember(budgetId, memberId)
        return ResponseEntity.ok(ApiResponse(success = true, message = "Member removed successfully"))
    }
}
