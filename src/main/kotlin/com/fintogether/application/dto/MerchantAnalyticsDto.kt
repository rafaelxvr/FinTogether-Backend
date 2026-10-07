package com.fintogether.application.dto

import java.math.BigDecimal
import java.util.UUID

data class MerchantSpendingSummary(
    val merchantId: UUID,
    val merchantName: String,
    val totalAmount: BigDecimal,
    val transactionCount: Long,
    val percentage: BigDecimal
)

data class TopMerchantsResponse(
    val period: String,
    val summaries: List<MerchantSpendingSummary>,
    val totalSpend: BigDecimal
)
