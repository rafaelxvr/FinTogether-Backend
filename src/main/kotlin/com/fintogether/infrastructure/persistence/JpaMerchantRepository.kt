package com.fintogether.infrastructure.persistence

import com.fintogether.domain.entity.Merchant
import com.fintogether.domain.repository.MerchantRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

interface JpaMerchantRepositoryInterface : JpaRepository<Merchant, UUID> {
    fun findByBudgetId(budgetId: UUID): List<Merchant>
    fun findByBudgetId(budgetId: UUID, pageable: Pageable): Page<Merchant>
    fun findByBudgetIdAndNameContainingIgnoreCase(budgetId: UUID, query: String): List<Merchant>
    fun findByBudgetIdAndName(budgetId: UUID, name: String): Merchant?
}

@Repository
class JpaMerchantRepository(
    private val jpaRepository: JpaMerchantRepositoryInterface
) : MerchantRepository {

    override fun save(merchant: Merchant): Merchant = jpaRepository.save(merchant)

    override fun findById(id: UUID): Merchant? = jpaRepository.findById(id).orElse(null)

    override fun findByBudgetId(budgetId: UUID): List<Merchant> = jpaRepository.findByBudgetId(budgetId)

    override fun findByBudgetId(budgetId: UUID, pageable: Pageable): Page<Merchant> =
        jpaRepository.findByBudgetId(budgetId, pageable)

    override fun findByBudgetIdAndNameContainingIgnoreCase(budgetId: UUID, query: String): List<Merchant> =
        jpaRepository.findByBudgetIdAndNameContainingIgnoreCase(budgetId, query)

    override fun findByBudgetIdAndName(budgetId: UUID, name: String): Merchant? =
        jpaRepository.findByBudgetIdAndName(budgetId, name)

    override fun delete(merchant: Merchant) = jpaRepository.delete(merchant)
}
