package com.fintogether.application.service

import com.fintogether.application.dto.CategoryResponse
import com.fintogether.application.dto.CreateCategoryRequest
import com.fintogether.application.dto.UpdateCategoryRequest
import com.fintogether.application.mapper.CategoryMapper
import com.fintogether.domain.entity.Category
import com.fintogether.domain.entity.CategoryType
import com.fintogether.application.dto.PagedResponse
import com.fintogether.domain.exception.AccessDeniedException
import com.fintogether.domain.exception.EntityNotFoundException
import com.fintogether.domain.exception.InvalidOperationException
import com.fintogether.domain.repository.BudgetRepository
import com.fintogether.domain.repository.CategoryRepository
import com.fintogether.domain.repository.UserRepository
import com.fintogether.infrastructure.security.UserPrincipal
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class CategoryService(
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository,
    private val userRepository: UserRepository,
    private val categoryMapper: CategoryMapper
) {

    private val logger = LoggerFactory.getLogger(CategoryService::class.java)

    @Transactional
    fun createCategory(budgetId: UUID, request: CreateCategoryRequest): CategoryResponse {
        logger.info("Creating category '${request.name}' for budget: $budgetId")

        val budget = budgetRepository.findById(budgetId)
            ?: throw EntityNotFoundException("Budget", budgetId)

        validateBudgetAccess(budget.id)

        // Validate parent category if provided
        val parentCategory = request.parentId?.let { parentId ->
            val parent = findCategoryById(parentId)
            if (parent.budget.id != budgetId) {
                logger.warn("Parent category $parentId belongs to different budget")
                throw InvalidOperationException("Parent category must belong to the same budget", "createCategory")
            }
            if (parent.type != request.type) {
                logger.warn("Parent category type mismatch: ${parent.type} != ${request.type}")
                throw InvalidOperationException("Parent category must have the same type", "createCategory")
            }
            parent
        }

        val category = Category(
            budget = budget,
            name = request.name,
            type = request.type,
            parent = parentCategory,
            icon = request.icon,
            color = request.color,
            limitAmount = request.limitAmount,
            isEssential = request.isEssential
        )

        val savedCategory = categoryRepository.save(category)
        logger.info("Category created successfully: ${savedCategory.id}")
        return categoryMapper.toResponse(savedCategory)
    }

    @Transactional(readOnly = true)
    fun getCategoryById(id: UUID): CategoryResponse {
        logger.debug("Getting category by id: $id")
        val category = findCategoryById(id)
        validateBudgetAccess(category.budget.id)
        return categoryMapper.toResponse(category)
    }

    @Transactional(readOnly = true)
    fun getCategoriesByBudget(budgetId: UUID, type: CategoryType? = null, updatedSince: Instant? = null): List<CategoryResponse> {
        logger.debug("Getting categories for budget: $budgetId, type: $type, updatedSince: $updatedSince")
        validateBudgetAccess(budgetId)

        // TODO: filter by updatedSince once updatedAt field is added to entities
        val categories = if (type != null) {
            categoryRepository.findByBudgetIdAndType(budgetId, type)
        } else {
            categoryRepository.findByBudgetId(budgetId)
        }

        return categories.map { categoryMapper.toResponse(it) }
    }

    @Transactional(readOnly = true)
    fun getCategoriesByBudget(budgetId: UUID, pageable: Pageable): PagedResponse<CategoryResponse> {
        logger.debug("Getting paginated categories for budget: $budgetId, page: ${pageable.pageNumber}, size: ${pageable.pageSize}")
        validateBudgetAccess(budgetId)
        val page = categoryRepository.findByBudgetId(budgetId, pageable)
        return PagedResponse.from(page) { categoryMapper.toResponse(it) }
    }

    @Transactional(readOnly = true)
    fun getRootCategories(budgetId: UUID): List<CategoryResponse> {
        logger.debug("Getting root categories for budget: $budgetId")
        validateBudgetAccess(budgetId)

        val budget = budgetRepository.findById(budgetId)
            ?: throw EntityNotFoundException("Budget", budgetId)

        val rootCategories = categoryRepository.findByBudgetAndParentIsNull(budget)
        return rootCategories.map { categoryMapper.toResponse(it) }
    }

    @Transactional
    fun updateCategory(id: UUID, request: UpdateCategoryRequest): CategoryResponse {
        logger.info("Updating category: $id")
        val category = findCategoryById(id)
        validateBudgetAccess(category.budget.id)

        // Validate parent category if provided
        if (request.parentId != null) {
            if (request.parentId == id) {
                logger.warn("Category $id cannot be its own parent")
                throw InvalidOperationException("Category cannot be its own parent", "updateCategory")
            }

            val parentCategory = findCategoryById(request.parentId)
            if (parentCategory.budget.id != category.budget.id) {
                logger.warn("Parent category ${request.parentId} belongs to different budget")
                throw InvalidOperationException("Parent category must belong to the same budget", "updateCategory")
            }
            if (parentCategory.type != category.type) {
                logger.warn("Parent category type mismatch: ${parentCategory.type} != ${category.type}")
                throw InvalidOperationException("Parent category must have the same type", "updateCategory")
            }

            // Check for circular reference
            if (isDescendant(parentCategory, category)) {
                logger.warn("Circular reference detected between category $id and parent ${request.parentId}")
                throw InvalidOperationException("Cannot create circular parent-child relationship", "updateCategory")
            }

            category.updateParent(parentCategory)
        } else if (request.parentId == null && category.parent != null) {
            // Explicitly removing parent
            category.updateParent(null)
        }

        category.update(
            name = request.name,
            icon = request.icon,
            color = request.color,
            limitAmount = request.limitAmount,
            isEssential = request.isEssential
        )

        val updatedCategory = categoryRepository.save(category)
        logger.info("Category updated successfully: $id")
        return categoryMapper.toResponse(updatedCategory)
    }

    @Transactional
    fun deleteCategory(id: UUID) {
        logger.info("Deleting category: $id")
        val category = findCategoryById(id)
        validateBudgetAccess(category.budget.id)

        if (category.children.isNotEmpty()) {
            logger.warn("Cannot delete category $id with ${category.children.size} subcategories")
            throw InvalidOperationException("Cannot delete category with subcategories", "deleteCategory")
        }

        categoryRepository.delete(category)
        logger.info("Category deleted successfully: $id")
    }

    @Transactional
    fun archiveCategory(id: UUID): CategoryResponse {
        logger.info("Archiving category: $id")
        val category = findCategoryById(id)
        validateBudgetAccess(category.budget.id)

        category.archive()
        val updatedCategory = categoryRepository.save(category)
        logger.info("Category archived successfully: $id")
        return categoryMapper.toResponse(updatedCategory)
    }

    @Transactional
    fun unarchiveCategory(id: UUID): CategoryResponse {
        logger.info("Unarchiving category: $id")
        val category = findCategoryById(id)
        validateBudgetAccess(category.budget.id)

        category.unarchive()
        val updatedCategory = categoryRepository.save(category)
        logger.info("Category unarchived successfully: $id")
        return categoryMapper.toResponse(updatedCategory)
    }

    private fun findCategoryById(id: UUID): Category {
        return categoryRepository.findById(id)
            ?: throw EntityNotFoundException("Category", id)
    }

    private fun isDescendant(potentialDescendant: Category, ancestor: Category): Boolean {
        var current: Category? = potentialDescendant
        while (current != null) {
            if (current.id == ancestor.id) {
                return true
            }
            current = current.parent
        }
        return false
    }

    private fun validateBudgetAccess(budgetId: UUID) {
        val currentUser = getCurrentUser()
        val user = userRepository.findById(currentUser.id)
            ?: throw EntityNotFoundException("User", currentUser.id)

        val budget = budgetRepository.findById(budgetId)
            ?: throw EntityNotFoundException("Budget", budgetId)

        if (!budget.hasMember(user) && !budget.isOwner(user)) {
            logger.warn("Access denied for user ${currentUser.id} to budget $budgetId")
            throw AccessDeniedException("You don't have access to this budget", "Budget", budgetId)
        }
    }

    private fun getCurrentUser(): UserPrincipal {
        val authentication = SecurityContextHolder.getContext().authentication
        return authentication.principal as UserPrincipal
    }
}
