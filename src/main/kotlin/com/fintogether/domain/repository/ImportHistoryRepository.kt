package com.fintogether.domain.repository

import com.fintogether.domain.entity.ImportHistory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

interface ImportHistoryRepository {
    fun save(history: ImportHistory): ImportHistory
    fun findById(id: UUID): ImportHistory?
    fun findByBudgetId(budgetId: UUID, pageable: Pageable): Page<ImportHistory>
}
