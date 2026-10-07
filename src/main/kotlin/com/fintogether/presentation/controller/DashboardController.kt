package com.fintogether.presentation.controller

import com.fintogether.application.dto.*
import com.fintogether.application.service.DashboardService
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
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Dashboard analytics and insights")
@SecurityRequirement(name = "bearerAuth")
class DashboardController(
    private val dashboardService: DashboardService
) {

    @GetMapping("/summary")
    @Operation(
        summary = "Get dashboard summary",
        description = "Get comprehensive dashboard summary including income, expenses, category breakdown, and recent transactions for a specific budget and month"
    )
    fun getDashboardSummary(
        @RequestParam budgetId: UUID,
        @RequestParam(required = false) month: String?,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<DashboardSummaryResponse> {
        val yearMonth = month?.let { YearMonth.parse(it) } ?: YearMonth.now()
        val summary = dashboardService.getDashboardSummary(budgetId, yearMonth, userPrincipal.id)
        return ResponseEntity.ok(summary)
    }

    @GetMapping("/cashflow")
    @Operation(
        summary = "Get cashflow trend",
        description = "Get monthly income/expense trend data for the last N months (default 6)"
    )
    fun getCashflowTrend(
        @RequestParam budgetId: UUID,
        @RequestParam(defaultValue = "6") months: Int,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<CashflowTrendResponse> {
        val trend = dashboardService.getCashflowTrend(budgetId, months, userPrincipal.id)
        return ResponseEntity.ok(trend)
    }

    @GetMapping("/category-breakdown")
    @Operation(
        summary = "Get category breakdown",
        description = "Get top spending and earning categories for a specific month with percentage breakdown"
    )
    fun getCategoryBreakdown(
        @RequestParam budgetId: UUID,
        @RequestParam(required = false) month: String?,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<TopCategoriesResponse> {
        val yearMonth = month?.let { YearMonth.parse(it) } ?: YearMonth.now()
        val breakdown = dashboardService.getCategoryBreakdown(budgetId, yearMonth, userPrincipal.id)
        return ResponseEntity.ok(breakdown)
    }

    @GetMapping("/account-balance")
    @Operation(
        summary = "Get account balance summary",
        description = "Get total balance and breakdown by account type"
    )
    fun getAccountBalanceSummary(
        @RequestParam budgetId: UUID,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<AccountBalanceSummaryDto> {
        val summary = dashboardService.getAccountBalanceSummary(budgetId, userPrincipal.id)
        return ResponseEntity.ok(summary)
    }

    @GetMapping("/essential-breakdown")
    @Operation(
        summary = "Get essential vs non-essential breakdown",
        description = "Get spending breakdown split between essential (needs) and non-essential (wants) categories for a specific budget and month"
    )
    fun getEssentialBreakdown(
        @RequestParam budgetId: UUID,
        @RequestParam(required = false) month: String?,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<EssentialBreakdownResponse> {
        val yearMonth = month?.let { YearMonth.parse(it) } ?: YearMonth.now()
        val breakdown = dashboardService.getEssentialBreakdown(budgetId, yearMonth, userPrincipal.id)
        return ResponseEntity.ok(breakdown)
    }

    @GetMapping("/alerts")
    @Operation(
        summary = "Get dashboard alerts",
        description = "Get alerts for low balance accounts, exceeded category limits, and upcoming credit card due dates"
    )
    fun getDashboardAlerts(
        @RequestParam budgetId: UUID,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<DashboardAlertsResponse> {
        val alerts = dashboardService.getDashboardAlerts(budgetId, userPrincipal.id)
        return ResponseEntity.ok(alerts)
    }

    @GetMapping("/member-spending")
    @Operation(
        summary = "Get member spending",
        description = "Get per-member spending breakdown for shared budgets"
    )
    fun getMemberSpending(
        @RequestParam budgetId: UUID,
        @RequestParam(required = false) month: String?,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<MemberSpendingResponse> {
        val yearMonth = month?.let { YearMonth.parse(it) } ?: YearMonth.now()
        val spending = dashboardService.getMemberSpending(budgetId, yearMonth, userPrincipal.id)
        return ResponseEntity.ok(spending)
    }

    @GetMapping("/forecast")
    @Operation(
        summary = "Get expense forecast",
        description = "Get expense forecast based on current month spending pace"
    )
    fun getExpenseForecast(
        @RequestParam budgetId: UUID,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<ExpenseForecastDto> {
        val forecast = dashboardService.getExpenseForecast(budgetId, userPrincipal.id)
        return ResponseEntity.ok(forecast)
    }

    @GetMapping("/income-by-member")
    @Operation(
        summary = "Get income by member",
        description = "Get income distribution grouped by member for shared budgets"
    )
    fun getIncomeByMember(
        @RequestParam budgetId: UUID,
        @RequestParam(required = false) month: String?,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<IncomeByMemberResponse> {
        val yearMonth = month?.let { YearMonth.parse(it) } ?: YearMonth.now()
        val report = dashboardService.getIncomeByMember(budgetId, yearMonth, userPrincipal.id)
        return ResponseEntity.ok(report)
    }
}
