package com.fintogether.presentation.controller

import com.fintogether.application.dto.*
import com.fintogether.application.service.RecurringTransactionTemplateService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api")
@Tag(name = "Recurring Transactions", description = "Recurring transaction template management endpoints")
@SecurityRequirement(name = "bearerAuth")
class RecurringTransactionController(
    private val templateService: RecurringTransactionTemplateService
) {

    @PostMapping("/budgets/{budgetId}/recurring-templates")
    @Operation(summary = "Create a new recurring transaction template")
    fun createTemplate(
        @PathVariable budgetId: UUID,
        @Valid @RequestBody request: CreateRecurringTemplateRequest
    ): ResponseEntity<ApiResponse<RecurringTemplateResponse>> {
        val template = templateService.createTemplate(budgetId, request)
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse(success = true, data = template, message = "Recurring template created successfully"))
    }

    @GetMapping("/budgets/{budgetId}/recurring-templates")
    @Operation(summary = "Get all recurring templates for a budget")
    fun getTemplatesByBudget(
        @PathVariable budgetId: UUID
    ): ResponseEntity<ApiResponse<List<RecurringTemplateResponse>>> {
        val templates = templateService.getTemplatesByBudget(budgetId)
        return ResponseEntity.ok(ApiResponse(success = true, data = templates))
    }

    @GetMapping("/budgets/{budgetId}/recurring-templates/fixed-expenses")
    @Operation(summary = "Get all fixed expense templates for a budget")
    fun getFixedExpenses(
        @PathVariable budgetId: UUID
    ): ResponseEntity<ApiResponse<List<RecurringTemplateResponse>>> {
        val templates = templateService.getFixedExpensesByBudget(budgetId)
        return ResponseEntity.ok(ApiResponse(success = true, data = templates))
    }

    @GetMapping("/budgets/{budgetId}/recurring-templates/fixed-expenses/summary")
    @Operation(summary = "Get fixed expense summary for a budget")
    fun getFixedExpenseSummary(
        @PathVariable budgetId: UUID
    ): ResponseEntity<ApiResponse<FixedExpenseSummaryResponse>> {
        val summary = templateService.getFixedExpenseSummary(budgetId)
        return ResponseEntity.ok(ApiResponse(success = true, data = summary))
    }

    @GetMapping("/recurring-templates/{id}")
    @Operation(summary = "Get a recurring template by ID")
    fun getTemplateById(
        @PathVariable id: UUID
    ): ResponseEntity<ApiResponse<RecurringTemplateResponse>> {
        val template = templateService.getTemplateById(id)
        return ResponseEntity.ok(ApiResponse(success = true, data = template))
    }

    @PutMapping("/recurring-templates/{id}")
    @Operation(summary = "Update a recurring template")
    fun updateTemplate(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateRecurringTemplateRequest
    ): ResponseEntity<ApiResponse<RecurringTemplateResponse>> {
        val template = templateService.updateTemplate(id, request)
        return ResponseEntity.ok(ApiResponse(success = true, data = template, message = "Recurring template updated successfully"))
    }

    @PutMapping("/recurring-templates/{id}/deactivate")
    @Operation(summary = "Deactivate a recurring template")
    fun deactivateTemplate(
        @PathVariable id: UUID
    ): ResponseEntity<ApiResponse<RecurringTemplateResponse>> {
        val template = templateService.deactivateTemplate(id)
        return ResponseEntity.ok(ApiResponse(success = true, data = template, message = "Recurring template deactivated successfully"))
    }

    @DeleteMapping("/recurring-templates/{id}")
    @Operation(summary = "Delete a recurring template")
    fun deleteTemplate(
        @PathVariable id: UUID
    ): ResponseEntity<ApiResponse<Nothing>> {
        templateService.deleteTemplate(id)
        return ResponseEntity.ok(ApiResponse(success = true, message = "Recurring template deleted successfully"))
    }
}
