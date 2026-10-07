package com.fintogether.presentation.controller

import com.fintogether.application.dto.*
import com.fintogether.application.service.TransactionService
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
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import com.fintogether.domain.entity.TransactionType

@RestController
@RequestMapping("/api")
@Tag(name = "Transactions", description = "Transaction management endpoints")
@SecurityRequirement(name = "bearerAuth")
class TransactionController(
    private val transactionService: TransactionService
) {

    @PostMapping("/budgets/{budgetId}/transactions")
    @Operation(summary = "Create a new transaction in a budget")
    fun createTransaction(
        @PathVariable budgetId: UUID,
        @Valid @RequestBody request: CreateTransactionRequest
    ): ResponseEntity<ApiResponse<TransactionResponse>> {
        val transaction = transactionService.createTransaction(budgetId, request)
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse(success = true, data = transaction, message = "Transaction created successfully"))
    }

    @PostMapping("/budgets/{budgetId}/transactions/bulk")
    @Operation(summary = "Create multiple transactions in a budget")
    fun bulkCreateTransactions(
        @PathVariable budgetId: UUID,
        @Valid @RequestBody request: BulkCreateTransactionRequest
    ): ResponseEntity<ApiResponse<BulkTransactionResponse>> {
        val result = transactionService.bulkCreateTransactions(budgetId, request)
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse(
                success = true,
                data = result,
                message = "${result.created.size} transactions created, ${result.failed.size} failed"
            ))
    }

    @GetMapping("/budgets/{budgetId}/transactions")
    @Operation(summary = "Get all transactions for a budget, optionally filtered by date range. Supports pagination.")
    fun getTransactionsByBudget(
        @PathVariable budgetId: UUID,
        @Parameter(description = "Start date (inclusive)")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate?,
        @Parameter(description = "End date (inclusive)")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate?,
        @Parameter(description = "Page number (0-based)")
        @RequestParam(required = false) page: Int?,
        @Parameter(description = "Page size (default 20)")
        @RequestParam(required = false, defaultValue = "20") size: Int,
        @Parameter(description = "Only return records updated after this ISO-8601 timestamp")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) updatedSince: java.time.Instant?
    ): ResponseEntity<*> {
        return if (page != null) {
            val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "transactionDate"))
            val pagedResult = transactionService.getTransactionsByBudget(budgetId, startDate, endDate, pageable)
            ResponseEntity.ok(ApiResponse(success = true, data = pagedResult))
        } else {
            val transactions = transactionService.getTransactionsByBudget(budgetId, startDate, endDate, updatedSince)
            ResponseEntity.ok(ApiResponse(success = true, data = transactions))
        }
    }

    @GetMapping("/budgets/{budgetId}/transactions/search")
    @Operation(summary = "Search transactions by text and filters")
    fun searchTransactions(
        @PathVariable budgetId: UUID,
        @Parameter(description = "Search query (matches description, notes)")
        @RequestParam(required = false) q: String?,
        @Parameter(description = "Transaction type filter")
        @RequestParam(required = false) type: TransactionType?,
        @Parameter(description = "Category ID filter")
        @RequestParam(required = false) categoryId: UUID?,
        @Parameter(description = "Merchant ID filter")
        @RequestParam(required = false) merchantId: UUID?,
        @Parameter(description = "Minimum amount")
        @RequestParam(required = false) minAmount: BigDecimal?,
        @Parameter(description = "Maximum amount")
        @RequestParam(required = false) maxAmount: BigDecimal?,
        @Parameter(description = "Start date (inclusive)")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate?,
        @Parameter(description = "End date (inclusive)")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate?,
        @Parameter(description = "Page number (0-based)")
        @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Page size")
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponse<PagedResponse<TransactionResponse>>> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "transactionDate"))
        val results = transactionService.searchTransactions(
            budgetId, q, type, categoryId, merchantId,
            minAmount, maxAmount, startDate, endDate, pageable
        )
        return ResponseEntity.ok(ApiResponse(success = true, data = results))
    }

    @GetMapping("/transactions/{id}")
    @Operation(summary = "Get transaction by ID")
    fun getTransactionById(@PathVariable id: UUID): ResponseEntity<ApiResponse<TransactionResponse>> {
        val transaction = transactionService.getTransactionById(id)
        return ResponseEntity.ok(ApiResponse(success = true, data = transaction))
    }

    @GetMapping("/categories/{categoryId}/transactions")
    @Operation(summary = "Get all transactions for a category")
    fun getTransactionsByCategory(@PathVariable categoryId: UUID): ResponseEntity<ApiResponse<List<TransactionResponse>>> {
        val transactions = transactionService.getTransactionsByCategory(categoryId)
        return ResponseEntity.ok(ApiResponse(success = true, data = transactions))
    }

    @GetMapping("/cards/{cardId}/transactions")
    @Operation(summary = "Get all transactions for a credit card, optionally filtered by date range")
    fun getTransactionsByCard(
        @PathVariable cardId: UUID,
        @Parameter(description = "Start date (inclusive)")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate?,
        @Parameter(description = "End date (inclusive)")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate?
    ): ResponseEntity<ApiResponse<List<TransactionResponse>>> {
        val transactions = transactionService.getTransactionsByCard(cardId, startDate, endDate)
        return ResponseEntity.ok(ApiResponse(success = true, data = transactions))
    }

    @GetMapping("/installment-groups/{groupId}/transactions")
    @Operation(summary = "Get all transactions for an installment group")
    fun getTransactionsByInstallmentGroup(@PathVariable groupId: UUID): ResponseEntity<ApiResponse<List<TransactionResponse>>> {
        val transactions = transactionService.getTransactionsByInstallmentGroup(groupId)
        return ResponseEntity.ok(ApiResponse(success = true, data = transactions))
    }

    @PutMapping("/transactions/{id}")
    @Operation(summary = "Update transaction")
    fun updateTransaction(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateTransactionRequest
    ): ResponseEntity<ApiResponse<TransactionResponse>> {
        val transaction = transactionService.updateTransaction(id, request)
        return ResponseEntity.ok(ApiResponse(success = true, data = transaction, message = "Transaction updated successfully"))
    }

    @DeleteMapping("/transactions/{id}")
    @Operation(summary = "Delete transaction")
    fun deleteTransaction(@PathVariable id: UUID): ResponseEntity<ApiResponse<Nothing>> {
        transactionService.deleteTransaction(id)
        return ResponseEntity.ok(ApiResponse(success = true, message = "Transaction deleted successfully"))
    }

    @DeleteMapping("/installment-groups/{groupId}/transactions")
    @Operation(summary = "Delete all transactions in an installment group")
    fun deleteTransactionsByInstallmentGroup(@PathVariable groupId: UUID): ResponseEntity<ApiResponse<Nothing>> {
        transactionService.deleteTransactionsByInstallmentGroup(groupId)
        return ResponseEntity.ok(ApiResponse(success = true, message = "All installment transactions deleted successfully"))
    }
}
