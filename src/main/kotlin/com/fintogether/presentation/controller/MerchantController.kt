package com.fintogether.presentation.controller

import com.fintogether.application.dto.*
import com.fintogether.application.service.MerchantService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api")
@Tag(name = "Merchants", description = "Merchant management endpoints")
@SecurityRequirement(name = "bearerAuth")
class MerchantController(
    private val merchantService: MerchantService
) {

    @PostMapping("/budgets/{budgetId}/merchants")
    @Operation(summary = "Create a new merchant in a budget")
    fun createMerchant(
        @PathVariable budgetId: UUID,
        @Valid @RequestBody request: CreateMerchantRequest
    ): ResponseEntity<ApiResponse<MerchantResponse>> {
        val merchant = merchantService.createMerchant(budgetId, request)
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse(success = true, data = merchant, message = "Merchant created successfully"))
    }

    @GetMapping("/budgets/{budgetId}/merchants")
    @Operation(summary = "Get all merchants for a budget. Supports pagination.")
    fun getMerchantsByBudget(
        @PathVariable budgetId: UUID,
        @Parameter(description = "Page number (0-based)")
        @RequestParam(required = false) page: Int?,
        @Parameter(description = "Page size (default 20)")
        @RequestParam(required = false, defaultValue = "20") size: Int
    ): ResponseEntity<*> {
        return if (page != null) {
            val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"))
            val pagedResult = merchantService.getMerchantsByBudget(budgetId, pageable)
            ResponseEntity.ok(ApiResponse(success = true, data = pagedResult))
        } else {
            val merchants = merchantService.getMerchantsByBudget(budgetId)
            ResponseEntity.ok(ApiResponse(success = true, data = merchants))
        }
    }

    @GetMapping("/budgets/{budgetId}/merchants/search")
    @Operation(summary = "Search merchants by name (autocomplete)")
    fun searchMerchants(
        @PathVariable budgetId: UUID,
        @Parameter(description = "Search query")
        @RequestParam q: String
    ): ResponseEntity<ApiResponse<List<MerchantResponse>>> {
        val merchants = merchantService.searchMerchants(budgetId, q)
        return ResponseEntity.ok(ApiResponse(success = true, data = merchants))
    }

    @GetMapping("/merchants/{id}")
    @Operation(summary = "Get merchant by ID")
    fun getMerchantById(@PathVariable id: UUID): ResponseEntity<ApiResponse<MerchantResponse>> {
        val merchant = merchantService.getMerchantById(id)
        return ResponseEntity.ok(ApiResponse(success = true, data = merchant))
    }

    @PutMapping("/merchants/{id}")
    @Operation(summary = "Update merchant")
    fun updateMerchant(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateMerchantRequest
    ): ResponseEntity<ApiResponse<MerchantResponse>> {
        val merchant = merchantService.updateMerchant(id, request)
        return ResponseEntity.ok(ApiResponse(success = true, data = merchant, message = "Merchant updated successfully"))
    }

    @DeleteMapping("/merchants/{id}")
    @Operation(summary = "Delete merchant")
    fun deleteMerchant(@PathVariable id: UUID): ResponseEntity<ApiResponse<Nothing>> {
        merchantService.deleteMerchant(id)
        return ResponseEntity.ok(ApiResponse(success = true, message = "Merchant deleted successfully"))
    }

    @PostMapping("/merchants/{id}/archive")
    @Operation(summary = "Archive merchant")
    fun archiveMerchant(@PathVariable id: UUID): ResponseEntity<ApiResponse<MerchantResponse>> {
        val merchant = merchantService.archiveMerchant(id)
        return ResponseEntity.ok(ApiResponse(success = true, data = merchant, message = "Merchant archived"))
    }

    @PostMapping("/merchants/{id}/unarchive")
    @Operation(summary = "Unarchive merchant")
    fun unarchiveMerchant(@PathVariable id: UUID): ResponseEntity<ApiResponse<MerchantResponse>> {
        val merchant = merchantService.unarchiveMerchant(id)
        return ResponseEntity.ok(ApiResponse(success = true, data = merchant, message = "Merchant unarchived"))
    }

    @GetMapping("/budgets/{budgetId}/merchants/analytics/top")
    @Operation(summary = "Get top merchants by spending")
    fun getTopMerchants(
        @PathVariable budgetId: UUID,
        @Parameter(description = "Start date (inclusive)")
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @Parameter(description = "End date (inclusive)")
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate,
        @Parameter(description = "Number of top merchants to return (default 5)")
        @RequestParam(required = false, defaultValue = "5") limit: Int
    ): ResponseEntity<ApiResponse<TopMerchantsResponse>> {
        val topMerchants = merchantService.getTopMerchants(budgetId, startDate, endDate, limit)
        return ResponseEntity.ok(ApiResponse(success = true, data = topMerchants))
    }
}
