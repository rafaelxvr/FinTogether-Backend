package com.fintogether.presentation.controller

import com.fintogether.application.dto.*
import com.fintogether.application.service.CreditCardCostService
import com.fintogether.infrastructure.security.UserPrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.YearMonth
import java.util.*

@RestController
@RequestMapping("/api/credit-cards")
@Tag(name = "Credit Card Costs", description = "Credit card billing-cycle-aware cost aggregation")
@SecurityRequirement(name = "bearerAuth")
class CreditCardCostController(
    private val creditCardCostService: CreditCardCostService
) {

    @GetMapping("/costs/summary")
    @Operation(
        summary = "Get all cards cost summary",
        description = "Get cost summary for all active cards in a budget for a specific billing cycle month"
    )
    fun getAllCardsCostSummary(
        @RequestParam budgetId: UUID,
        @RequestParam(required = false) month: String?,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<AllCardsCostSummaryResponse> {
        val yearMonth = month?.let { YearMonth.parse(it) } ?: YearMonth.now()
        val summary = creditCardCostService.getAllCardsCostSummary(budgetId, yearMonth, userPrincipal.id)
        return ResponseEntity.ok(summary)
    }

    @GetMapping("/{id}/costs")
    @Operation(
        summary = "Get single card monthly cost",
        description = "Get the monthly cost for a single card within its billing cycle"
    )
    fun getCardMonthlyCost(
        @PathVariable id: UUID,
        @RequestParam(required = false) month: String?,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<CardMonthlyCostResponse> {
        val yearMonth = month?.let { YearMonth.parse(it) } ?: YearMonth.now()
        val cost = creditCardCostService.getCardMonthlyCost(id, yearMonth, userPrincipal.id)
        return ResponseEntity.ok(cost)
    }

    @GetMapping("/{id}/costs/comparison")
    @Operation(
        summary = "Get card cost comparison",
        description = "Get cost comparison for a single card across multiple billing cycle months"
    )
    fun getCardCostComparison(
        @PathVariable id: UUID,
        @RequestParam(defaultValue = "6") months: Int,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<CardCostComparisonResponse> {
        val comparison = creditCardCostService.getCardCostComparison(id, months, userPrincipal.id)
        return ResponseEntity.ok(comparison)
    }
}
