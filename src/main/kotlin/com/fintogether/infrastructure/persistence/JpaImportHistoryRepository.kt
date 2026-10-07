package com.fintogether.infrastructure.persistence

import com.fintogether.domain.entity.ImportHistory
import com.fintogether.domain.repository.ImportHistoryRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

interface JpaImportHistoryRepositoryInterface : JpaRepository<ImportHistory, UUID> {
    fun findByBudgetId(budgetId: UUID, pageable: Pageable): Page<ImportHistory>
}

@Repository
class JpaImportHistoryRepository(
    private val jpaRepository: JpaImportHistoryRepositoryInterface
) : ImportHistoryRepository {

    override fun save(history: ImportHistory): ImportHistory = jpaRepository.save(history)

    override fun findById(id: UUID): ImportHistory? = jpaRepository.findById(id).orElse(null)

    override fun findByBudgetId(budgetId: UUID, pageable: Pageable): Page<ImportHistory> =
        jpaRepository.findByBudgetId(budgetId, pageable)
}
