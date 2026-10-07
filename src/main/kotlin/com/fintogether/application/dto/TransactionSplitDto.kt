package com.fintogether.application.dto

import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

data class CreateSplitRequest(
    val userId: UUID,
    val amount: BigDecimal
)

data class SplitResponse(
    val id: UUID,
    val transactionId: UUID,
    val userId: UUID,
    val userName: String,
    val amount: BigDecimal,
    val isPaid: Boolean,
    val paidAt: LocalDateTime?,
    val createdAt: LocalDateTime
)

data class SplitBalanceEntry(
    val userId: UUID,
    val userName: String,
    val owesTo: UUID,
    val owesToName: String,
    val netAmount: BigDecimal
)

data class BalanceSummaryResponse(
    val budgetId: UUID,
    val entries: List<SplitBalanceEntry>
)
