package com.fintogether.application.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class CreateImportProfileRequest(
    @field:NotBlank(message = "Profile name is required")
    val name: String,

    val delimiter: String = ",",

    val encoding: String = "UTF-8",

    @field:NotNull(message = "Column mapping is required")
    val mapping: Map<String, Any>,

    val cardId: UUID? = null
)

data class ImportProfileResponse(
    val id: UUID,
    val budgetId: UUID,
    val name: String,
    val delimiter: String,
    val encoding: String,
    val mapping: Map<String, Any>,
    val rules: Map<String, Any>?,
    val cardId: UUID?,
    val createdAt: LocalDateTime
)

data class ImportPreviewRequest(
    val profileId: UUID? = null,
    val mapping: Map<String, Any>? = null
)

data class ImportPreviewRow(
    val rowNumber: Int,
    val date: LocalDate?,
    val description: String?,
    val amount: BigDecimal?,
    val type: String?,
    val matchedMerchantId: UUID? = null,
    val matchedMerchantName: String? = null,
    val matchedCategoryId: UUID? = null,
    val matchedCategoryName: String? = null,
    val isDuplicate: Boolean = false,
    val duplicateTransactionId: UUID? = null,
    val selected: Boolean = true
)

data class ImportPreviewResponse(
    val rows: List<ImportPreviewRow>,
    val totalRows: Int,
    val duplicateCount: Int,
    val profileUsed: String?
)

data class ConfirmImportRequest(
    val selectedRowNumbers: List<Int>,
    val accountId: UUID? = null,
    val profileId: UUID? = null,
    val mapping: Map<String, Any>? = null,
    val fileName: String? = null
)

data class ImportResultResponse(
    val totalImported: Int,
    val totalSkipped: Int,
    val importHistoryId: UUID
)

data class ImportHistoryResponse(
    val id: UUID,
    val fileName: String,
    val totalRows: Int,
    val importedRows: Int,
    val skippedRows: Int,
    val duplicateRows: Int,
    val status: String,
    val errorMessage: String?,
    val createdAt: LocalDateTime
)
