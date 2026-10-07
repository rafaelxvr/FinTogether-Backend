package com.fintogether.domain.repository

import com.fintogether.domain.entity.Budget
import com.fintogether.domain.entity.Category
import com.fintogether.domain.entity.CategoryType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

interface CategoryRepository {
    fun save(category: Category): Category
    fun saveAll(categories: List<Category>): List<Category>
    fun findById(id: UUID): Category?
    fun findByBudget(budget: Budget): List<Category>
    fun findByBudgetId(budgetId: UUID): List<Category>
    fun findByBudgetId(budgetId: UUID, pageable: Pageable): Page<Category>
    fun findByBudgetAndType(budget: Budget, type: CategoryType): List<Category>
    fun findByBudgetIdAndType(budgetId: UUID, type: CategoryType): List<Category>
    fun findByBudgetAndParentIsNull(budget: Budget): List<Category>
    fun delete(category: Category)
}
