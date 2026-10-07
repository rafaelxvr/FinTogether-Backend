package com.fintogether.presentation.controller

import com.fintogether.application.dto.*
import com.fintogether.application.service.TransactionSplitService
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
@Tag(name = "Transaction Splits", description = "Expense splitting management endpoints")
@SecurityRequirement(name = "bearerAuth")
class TransactionSplitController(
    private val transactionSplitService: TransactionSplitService
) {

    @PostMapping("/transactions/{transactionId}/splits")
    @Operation(summary = "Create splits for a transaction")
    fun createSplits(
        @PathVariable transactionId: UUID,
        @Valid @RequestBody splits: List<CreateSplitRequest>
    ): ResponseEntity<ApiResponse<List<SplitResponse>>> {
        val result = transactionSplitService.createSplits(transactionId, splits)
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse(success = true, data = result, message = "Splits created successfully"))
    }

    @GetMapping("/transactions/{transactionId}/splits")
    @Operation(summary = "Get all splits for a transaction")
    fun getSplitsForTransaction(
        @PathVariable transactionId: UUID
    ): ResponseEntity<ApiResponse<List<SplitResponse>>> {
        val splits = transactionSplitService.getSplitsForTransaction(transactionId)
        return ResponseEntity.ok(ApiResponse(success = true, data = splits))
    }

    @GetMapping("/budgets/{budgetId}/splits/balances")
    @Operation(summary = "Get balance summary for a budget")
    fun getBalances(
        @PathVariable budgetId: UUID
    ): ResponseEntity<ApiResponse<BalanceSummaryResponse>> {
        val balances = transactionSplitService.getBalances(budgetId)
        return ResponseEntity.ok(ApiResponse(success = true, data = balances))
    }

    @PostMapping("/splits/{splitId}/settle")
    @Operation(summary = "Mark a split as settled/paid")
    fun settleSplit(
        @PathVariable splitId: UUID
    ): ResponseEntity<ApiResponse<SplitResponse>> {
        val split = transactionSplitService.settleSplit(splitId)
        return ResponseEntity.ok(ApiResponse(success = true, data = split, message = "Split settled successfully"))
    }
}
