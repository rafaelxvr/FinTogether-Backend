package com.fintogether.infrastructure.persistence

import com.fintogether.domain.entity.Budget
import com.fintogether.domain.entity.Category
import com.fintogether.domain.entity.CategoryType
import com.fintogether.domain.repository.CategoryRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

interface JpaCategoryRepositoryInterface : JpaRepository<Category, UUID> {
    fun findByBudget(budget: Budget): List<Category>
    fun findByBudgetId(budgetId: UUID): List<Category>
    fun findByBudgetId(budgetId: UUID, pageable: Pageable): Page<Category>
    fun findByBudgetAndType(budget: Budget, type: CategoryType): List<Category>
    fun findByBudgetIdAndType(budgetId: UUID, type: CategoryType): List<Category>
    fun findByBudgetAndParentIsNull(budget: Budget): List<Category>
}

@Repository
class JpaCategoryRepository(
    private val jpaRepository: JpaCategoryRepositoryInterface
) : CategoryRepository {

    override fun save(category: Category): Category = jpaRepository.save(category)

    override fun saveAll(categories: List<Category>): List<Category> = jpaRepository.saveAll(categories)

    override fun findById(id: UUID): Category? = jpaRepository.findById(id).orElse(null)

    override fun findByBudget(budget: Budget): List<Category> = jpaRepository.findByBudget(budget)

    override fun findByBudgetId(budgetId: UUID): List<Category> = jpaRepository.findByBudgetId(budgetId)

    override fun findByBudgetId(budgetId: UUID, pageable: Pageable): Page<Category> =
        jpaRepository.findByBudgetId(budgetId, pageable)

    override fun findByBudgetAndType(budget: Budget, type: CategoryType): List<Category> =
        jpaRepository.findByBudgetAndType(budget, type)

    override fun findByBudgetIdAndType(budgetId: UUID, type: CategoryType): List<Category> =
        jpaRepository.findByBudgetIdAndType(budgetId, type)

    override fun findByBudgetAndParentIsNull(budget: Budget): List<Category> =
        jpaRepository.findByBudgetAndParentIsNull(budget)

    override fun delete(category: Category) = jpaRepository.delete(category)
}
