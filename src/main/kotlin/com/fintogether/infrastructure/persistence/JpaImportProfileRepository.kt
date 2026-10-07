package com.fintogether.infrastructure.persistence

import com.fintogether.domain.entity.ImportProfile
import com.fintogether.domain.repository.ImportProfileRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

interface JpaImportProfileRepositoryInterface : JpaRepository<ImportProfile, UUID> {
    fun findByBudgetId(budgetId: UUID): List<ImportProfile>
}

@Repository
class JpaImportProfileRepository(
    private val jpaRepository: JpaImportProfileRepositoryInterface
) : ImportProfileRepository {

    override fun save(profile: ImportProfile): ImportProfile = jpaRepository.save(profile)

    override fun findById(id: UUID): ImportProfile? = jpaRepository.findById(id).orElse(null)

    override fun findByBudgetId(budgetId: UUID): List<ImportProfile> = jpaRepository.findByBudgetId(budgetId)

    override fun delete(profile: ImportProfile) = jpaRepository.delete(profile)
}
