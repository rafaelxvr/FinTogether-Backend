package com.fintogether.presentation.controller

import com.fintogether.application.dto.*
import com.fintogether.application.service.CreditCardInvoiceService
import com.fintogether.application.service.CreditCardService
import com.fintogether.infrastructure.security.UserPrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.time.YearMonth
import java.util.UUID

/**
 * Credit Card Controller
 * REST endpoints for credit card management
 */
@RestController
@RequestMapping("/api/credit-cards")
@Tag(name = "Credit Cards", description = "Credit card management endpoints")
@SecurityRequirement(name = "bearerAuth")
class CreditCardController(
    private val creditCardService: CreditCardService,
    private val creditCardInvoiceService: CreditCardInvoiceService
) {

    @PostMapping
    @Operation(summary = "Create a new credit card")
    fun createCreditCard(
        @RequestBody request: CreateCreditCardRequest,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<CreditCardResponse> {
        val response = creditCardService.createCreditCard(request, userPrincipal.id)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get credit card by ID")
    fun getCreditCardById(
        @PathVariable id: UUID,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<CreditCardResponse> {
        val response = creditCardService.getCreditCardById(id, userPrincipal.id)
        return ResponseEntity.ok(response)
    }

    @GetMapping
    @Operation(summary = "Get all credit cards for a budget. Supports pagination.")
    fun getCreditCards(
        @RequestParam budgetId: UUID,
        @RequestParam(required = false, defaultValue = "false") activeOnly: Boolean,
        @Parameter(description = "Page number (0-based)")
        @RequestParam(required = false) page: Int?,
        @Parameter(description = "Page size (default 20)")
        @RequestParam(required = false, defaultValue = "20") size: Int,
        @Parameter(description = "Only return records updated after this ISO-8601 timestamp")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) updatedSince: Instant?,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<*> {
        return if (activeOnly) {
            val response = creditCardService.getActiveCreditCards(budgetId, userPrincipal.id)
            ResponseEntity.ok(response)
        } else if (page != null) {
            val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"))
            val pagedResult = creditCardService.getCreditCardsByBudget(budgetId, userPrincipal.id, pageable)
            ResponseEntity.ok(pagedResult)
        } else {
            val response = creditCardService.getCreditCardsByBudget(budgetId, userPrincipal.id, updatedSince)
            ResponseEntity.ok(response)
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update credit card")
    fun updateCreditCard(
        @PathVariable id: UUID,
        @RequestBody request: UpdateCreditCardRequest,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<CreditCardResponse> {
        val response = creditCardService.updateCreditCard(id, request, userPrincipal.id)
        return ResponseEntity.ok(response)
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete credit card")
    fun deleteCreditCard(
        @PathVariable id: UUID,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<Void> {
        creditCardService.deleteCreditCard(id, userPrincipal.id)
        return ResponseEntity.noContent().build()
    }

    @PutMapping("/{id}/toggle-status")
    @Operation(summary = "Toggle credit card active status")
    fun toggleStatus(
        @PathVariable id: UUID,
        @RequestBody request: ToggleCreditCardStatusRequest,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<CreditCardResponse> {
        val response = creditCardService.toggleStatus(id, request, userPrincipal.id)
        return ResponseEntity.ok(response)
    }

    @PutMapping("/{id}/link-account")
    @Operation(summary = "Link an account to credit card for payments")
    fun linkAccount(
        @PathVariable id: UUID,
        @RequestBody request: LinkAccountRequest,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<CreditCardResponse> {
        val response = creditCardService.linkAccount(id, request, userPrincipal.id)
        return ResponseEntity.ok(response)
    }

    @DeleteMapping("/{id}/unlink-account")
    @Operation(summary = "Unlink account from credit card")
    fun unlinkAccount(
        @PathVariable id: UUID,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<CreditCardResponse> {
        val response = creditCardService.unlinkAccount(id, userPrincipal.id)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/{id}/invoice")
    @Operation(summary = "Get credit card invoice for a specific month")
    fun getInvoice(
        @PathVariable id: UUID,
        @Parameter(description = "Reference month in yyyy-MM format (e.g. 2026-03)")
        @RequestParam month: String,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<CreditCardInvoiceResponse> {
        val yearMonth = YearMonth.parse(month)
        val response = creditCardInvoiceService.getInvoice(id, yearMonth, userPrincipal.id)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/{id}/invoices")
    @Operation(summary = "Get credit card invoice summaries for the last N months")
    fun getInvoiceSummaries(
        @PathVariable id: UUID,
        @Parameter(description = "Number of months to look back (default 6)")
        @RequestParam(required = false, defaultValue = "6") monthsBack: Int,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<InvoiceSummaryResponse> {
        val response = creditCardInvoiceService.getInvoiceSummaries(id, monthsBack, userPrincipal.id)
        return ResponseEntity.ok(response)
    }
}
