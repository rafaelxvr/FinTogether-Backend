package com.fintogether.domain.repository

import com.fintogether.domain.entity.ImportProfile
import java.util.UUID

interface ImportProfileRepository {
    fun save(profile: ImportProfile): ImportProfile
    fun findById(id: UUID): ImportProfile?
    fun findByBudgetId(budgetId: UUID): List<ImportProfile>
    fun delete(profile: ImportProfile)
}
