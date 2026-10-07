package com.fintogether.domain.repository

import com.fintogether.domain.entity.Merchant
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

interface MerchantRepository {
    fun save(merchant: Merchant): Merchant
    fun findById(id: UUID): Merchant?
    fun findByBudgetId(budgetId: UUID): List<Merchant>
    fun findByBudgetId(budgetId: UUID, pageable: Pageable): Page<Merchant>
    fun findByBudgetIdAndNameContainingIgnoreCase(budgetId: UUID, query: String): List<Merchant>
    fun findByBudgetIdAndName(budgetId: UUID, name: String): Merchant?
    fun delete(merchant: Merchant)
}
