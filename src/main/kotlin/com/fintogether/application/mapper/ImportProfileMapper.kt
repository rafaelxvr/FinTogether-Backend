package com.fintogether.application.mapper

import com.fintogether.application.dto.ImportHistoryResponse
import com.fintogether.application.dto.ImportProfileResponse
import com.fintogether.domain.entity.ImportHistory
import com.fintogether.domain.entity.ImportProfile
import org.springframework.stereotype.Component

@Component
class ImportProfileMapper {

    fun toResponse(profile: ImportProfile): ImportProfileResponse {
        return ImportProfileResponse(
            id = profile.id,
            budgetId = profile.budget.id,
            name = profile.name,
            delimiter = profile.delimiter,
            encoding = profile.encoding,
            mapping = profile.mapping,
            rules = profile.rules,
            cardId = profile.card?.id,
            createdAt = profile.createdAt
        )
    }

    fun toHistoryResponse(history: ImportHistory): ImportHistoryResponse {
        return ImportHistoryResponse(
            id = history.id,
            fileName = history.fileName,
            totalRows = history.totalRows,
            importedRows = history.importedRows,
            skippedRows = history.skippedRows,
            duplicateRows = history.duplicateRows,
            status = history.status.name,
            errorMessage = history.errorMessage,
            createdAt = history.createdAt
        )
    }
}
