package com.fintogether.presentation.controller

import com.fintogether.application.dto.*
import com.fintogether.application.service.CategoryService
import com.fintogether.domain.entity.CategoryType
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
@RequestMapping("/api")
@Tag(name = "Categories", description = "Category management endpoints")
@SecurityRequirement(name = "bearerAuth")
class CategoryController(
    private val categoryService: CategoryService
) {

    @PostMapping("/budgets/{budgetId}/categories")
    @Operation(summary = "Create a new category in a budget")
    fun createCategory(
        @PathVariable budgetId: UUID,
        @Valid @RequestBody request: CreateCategoryRequest
    ): ResponseEntity<ApiResponse<CategoryResponse>> {
        val category = categoryService.createCategory(budgetId, request)
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse(success = true, data = category, message = "Category created successfully"))
    }

    @GetMapping("/budgets/{budgetId}/categories")
    @Operation(summary = "Get all categories for a budget, optionally filtered by type. Supports pagination.")
    fun getCategoriesByBudget(
        @PathVariable budgetId: UUID,
        @Parameter(description = "Filter by category type (INCOME, EXPENSE, TRANSFER)")
        @RequestParam(required = false) type: CategoryType?,
        @Parameter(description = "Page number (0-based)")
        @RequestParam(required = false) page: Int?,
        @Parameter(description = "Page size (default 20)")
        @RequestParam(required = false, defaultValue = "20") size: Int,
        @Parameter(description = "Only return records updated after this ISO-8601 timestamp")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) updatedSince: Instant?
    ): ResponseEntity<*> {
        return if (page != null) {
            val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"))
            val pagedResult = categoryService.getCategoriesByBudget(budgetId, pageable)
            ResponseEntity.ok(ApiResponse(success = true, data = pagedResult))
        } else {
            val categories = categoryService.getCategoriesByBudget(budgetId, type, updatedSince)
            ResponseEntity.ok(ApiResponse(success = true, data = categories))
        }
    }

    @GetMapping("/budgets/{budgetId}/categories/root")
    @Operation(summary = "Get all root categories (without parent) for a budget")
    fun getRootCategories(@PathVariable budgetId: UUID): ResponseEntity<ApiResponse<List<CategoryResponse>>> {
        val categories = categoryService.getRootCategories(budgetId)
        return ResponseEntity.ok(ApiResponse(success = true, data = categories))
    }

    @GetMapping("/categories/{id}")
    @Operation(summary = "Get category by ID")
    fun getCategoryById(@PathVariable id: UUID): ResponseEntity<ApiResponse<CategoryResponse>> {
        val category = categoryService.getCategoryById(id)
        return ResponseEntity.ok(ApiResponse(success = true, data = category))
    }

    @PutMapping("/categories/{id}")
    @Operation(summary = "Update category")
    fun updateCategory(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateCategoryRequest
    ): ResponseEntity<ApiResponse<CategoryResponse>> {
        val category = categoryService.updateCategory(id, request)
        return ResponseEntity.ok(ApiResponse(success = true, data = category, message = "Category updated successfully"))
    }

    @DeleteMapping("/categories/{id}")
    @Operation(summary = "Delete category")
    fun deleteCategory(@PathVariable id: UUID): ResponseEntity<ApiResponse<Nothing>> {
        categoryService.deleteCategory(id)
        return ResponseEntity.ok(ApiResponse(success = true, message = "Category deleted successfully"))
    }

    @PostMapping("/categories/{id}/archive")
    @Operation(summary = "Archive category")
    fun archiveCategory(@PathVariable id: UUID): ResponseEntity<ApiResponse<CategoryResponse>> {
        val category = categoryService.archiveCategory(id)
        return ResponseEntity.ok(ApiResponse(success = true, data = category, message = "Category archived"))
    }

    @PostMapping("/categories/{id}/unarchive")
    @Operation(summary = "Unarchive category")
    fun unarchiveCategory(@PathVariable id: UUID): ResponseEntity<ApiResponse<CategoryResponse>> {
        val category = categoryService.unarchiveCategory(id)
        return ResponseEntity.ok(ApiResponse(success = true, data = category, message = "Category unarchived"))
    }
}
