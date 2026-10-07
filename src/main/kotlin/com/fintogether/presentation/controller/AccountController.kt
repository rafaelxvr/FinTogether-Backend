package com.fintogether.presentation.controller

import com.fintogether.application.dto.*
import com.fintogether.application.service.AccountService
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
@Tag(name = "Accounts", description = "Account management endpoints")
@SecurityRequirement(name = "bearerAuth")
class AccountController(
    private val accountService: AccountService
) {

    @PostMapping("/budgets/{budgetId}/accounts")
    @Operation(summary = "Create a new account in a budget")
    fun createAccount(
        @PathVariable budgetId: UUID,
        @Valid @RequestBody request: CreateAccountRequest
    ): ResponseEntity<ApiResponse<AccountResponse>> {
        val account = accountService.createAccount(budgetId, request)
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse(success = true, data = account, message = "Account created successfully"))
    }

    @GetMapping("/budgets/{budgetId}/accounts")
    @Operation(summary = "Get all accounts for a budget. Supports pagination.")
    fun getAccountsByBudget(
        @PathVariable budgetId: UUID,
        @Parameter(description = "Page number (0-based)")
        @RequestParam(required = false) page: Int?,
        @Parameter(description = "Page size (default 20)")
        @RequestParam(required = false, defaultValue = "20") size: Int,
        @Parameter(description = "Only return records updated after this ISO-8601 timestamp")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) updatedSince: Instant?
    ): ResponseEntity<*> {
        return if (page != null) {
            val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"))
            val pagedResult = accountService.getAccountsByBudget(budgetId, pageable)
            ResponseEntity.ok(ApiResponse(success = true, data = pagedResult))
        } else {
            val accounts = accountService.getAccountsByBudget(budgetId, updatedSince)
            ResponseEntity.ok(ApiResponse(success = true, data = accounts))
        }
    }

    @GetMapping("/accounts/{id}")
    @Operation(summary = "Get account by ID")
    fun getAccountById(@PathVariable id: UUID): ResponseEntity<ApiResponse<AccountResponse>> {
        val account = accountService.getAccountById(id)
        return ResponseEntity.ok(ApiResponse(success = true, data = account))
    }

    @PutMapping("/accounts/{id}")
    @Operation(summary = "Update account")
    fun updateAccount(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateAccountRequest
    ): ResponseEntity<ApiResponse<AccountResponse>> {
        val account = accountService.updateAccount(id, request)
        return ResponseEntity.ok(ApiResponse(success = true, data = account, message = "Account updated successfully"))
    }

    @DeleteMapping("/accounts/{id}")
    @Operation(summary = "Delete account")
    fun deleteAccount(@PathVariable id: UUID): ResponseEntity<ApiResponse<Nothing>> {
        accountService.deleteAccount(id)
        return ResponseEntity.ok(ApiResponse(success = true, message = "Account deleted successfully"))
    }

    @PostMapping("/accounts/{id}/deactivate")
    @Operation(summary = "Deactivate account")
    fun deactivateAccount(@PathVariable id: UUID): ResponseEntity<ApiResponse<AccountResponse>> {
        val account = accountService.deactivateAccount(id)
        return ResponseEntity.ok(ApiResponse(success = true, data = account, message = "Account deactivated"))
    }

    @PostMapping("/accounts/{id}/activate")
    @Operation(summary = "Activate account")
    fun activateAccount(@PathVariable id: UUID): ResponseEntity<ApiResponse<AccountResponse>> {
        val account = accountService.activateAccount(id)
        return ResponseEntity.ok(ApiResponse(success = true, data = account, message = "Account activated"))
    }

    @PostMapping("/accounts/transfer")
    @Operation(summary = "Transfer money between accounts")
    fun transfer(@Valid @RequestBody request: TransferRequest): ResponseEntity<ApiResponse<Nothing>> {
        accountService.transfer(request)
        return ResponseEntity.ok(ApiResponse(success = true, message = "Transfer completed successfully"))
    }
}
