package com.fintogether.application.service

import com.fintogether.application.dto.*
import com.fintogether.application.mapper.BudgetMapper
import com.fintogether.domain.entity.Budget
import com.fintogether.domain.entity.MemberRole
import com.fintogether.domain.exception.AccessDeniedException
import com.fintogether.domain.exception.EntityNotFoundException
import com.fintogether.domain.repository.BudgetRepository
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
class BudgetService(
    private val budgetRepository: BudgetRepository,
    private val userRepository: UserRepository,
    private val budgetMapper: BudgetMapper,
    private val seedCategoryService: SeedCategoryService
) {

    private val logger = LoggerFactory.getLogger(BudgetService::class.java)

    @Transactional
    fun createBudget(request: CreateBudgetRequest): BudgetResponse {
        val currentUser = getCurrentUser()
        logger.info("Creating budget '${request.name}' for user: ${currentUser.id}")

        val user = userRepository.findById(currentUser.id)
            ?: throw EntityNotFoundException("User", currentUser.id)

        val budget = Budget(
            name = request.name,
            owner = user,
            type = request.type
        )

        // Add owner as a member
        budget.addMember(user, MemberRole.OWNER)

        val savedBudget = budgetRepository.save(budget)
        logger.info("Budget created successfully: ${savedBudget.id}")

        // Seed default categories based on user locale
        val locale = request.locale ?: user.locale
        seedCategoryService.seedDefaultCategories(savedBudget, locale)

        return budgetMapper.toResponse(savedBudget)
    }

    @Transactional(readOnly = true)
    fun getBudgetById(id: UUID): BudgetResponse {
        logger.debug("Getting budget by id: $id")
        val budget = findBudgetById(id)
        validateUserAccess(budget)
        return budgetMapper.toResponse(budget)
    }

    @Transactional(readOnly = true)
    fun getAllBudgets(updatedSince: Instant? = null): List<BudgetResponse> {
        val currentUser = getCurrentUser()
        logger.debug("Getting all budgets for user: ${currentUser.id}, updatedSince: $updatedSince")
        // TODO: filter by updatedSince once updatedAt field is added to entities
        val budgets = budgetRepository.findByOwnerId(currentUser.id)
        return budgets.map { budgetMapper.toResponse(it) }
    }

    @Transactional(readOnly = true)
    fun getAllBudgets(pageable: Pageable): PagedResponse<BudgetResponse> {
        val currentUser = getCurrentUser()
        logger.debug("Getting paginated budgets for user: ${currentUser.id}, page: ${pageable.pageNumber}, size: ${pageable.pageSize}")
        val page = budgetRepository.findByOwnerId(currentUser.id, pageable)
        return PagedResponse.from(page) { budgetMapper.toResponse(it) }
    }

    @Transactional
    fun updateBudget(id: UUID, request: UpdateBudgetRequest): BudgetResponse {
        logger.info("Updating budget: $id")
        val budget = findBudgetById(id)
        validateOwnerAccess(budget)

        budget.updateName(request.name)
        budget.changeType(request.type)

        val updatedBudget = budgetRepository.save(budget)
        logger.info("Budget updated successfully: $id")
        return budgetMapper.toResponse(updatedBudget)
    }

    @Transactional
    fun deleteBudget(id: UUID) {
        logger.info("Deleting budget: $id")
        val budget = findBudgetById(id)
        validateOwnerAccess(budget)
        budgetRepository.delete(budget)
        logger.info("Budget deleted successfully: $id")
    }

    @Transactional
    fun addMember(budgetId: UUID, request: BudgetMemberRequest): BudgetMemberResponse {
        logger.info("Adding member '${request.email}' to budget: $budgetId")
        val budget = findBudgetById(budgetId)
        validateOwnerAccess(budget)

        val user = userRepository.findByEmail(request.email)
            ?: throw EntityNotFoundException("User", request.email)

        if (budget.hasMember(user)) {
            logger.warn("User ${request.email} is already a member of budget $budgetId")
            throw IllegalArgumentException("User is already a member of this budget")
        }

        val role = when (request.role.uppercase()) {
            "OWNER" -> MemberRole.OWNER
            "MEMBER" -> MemberRole.MEMBER
            "VIEWER" -> MemberRole.VIEWER
            else -> throw IllegalArgumentException("Invalid role: ${request.role}")
        }

        val member = budget.addMember(user, role)
        budgetRepository.save(budget)

        logger.info("Member added successfully to budget: $budgetId")
        return budgetMapper.toMemberResponse(member)
    }

    @Transactional(readOnly = true)
    fun getBudgetMembers(budgetId: UUID): List<BudgetMemberResponse> {
        logger.debug("Getting members for budget: $budgetId")
        val budget = findBudgetById(budgetId)
        validateUserAccess(budget)

        return budget.members.map { budgetMapper.toMemberResponse(it) }
    }

    @Transactional
    fun removeMember(budgetId: UUID, memberId: UUID) {
        logger.info("Removing member $memberId from budget: $budgetId")
        val budget = findBudgetById(budgetId)
        validateOwnerAccess(budget)

        val member = budget.members.find { it.id == memberId }
            ?: throw EntityNotFoundException("BudgetMember", memberId)

        if (member.isOwner()) {
            logger.warn("Attempted to remove owner from budget: $budgetId")
            throw IllegalArgumentException("Cannot remove the owner from the budget")
        }

        budget.removeMember(member)
        budgetRepository.save(budget)
        logger.info("Member removed successfully from budget: $budgetId")
    }

    private fun findBudgetById(id: UUID): Budget {
        return budgetRepository.findById(id)
            ?: throw EntityNotFoundException("Budget", id)
    }

    private fun validateUserAccess(budget: Budget) {
        val currentUser = getCurrentUser()
        val user = userRepository.findById(currentUser.id)
            ?: throw EntityNotFoundException("User", currentUser.id)

        if (!budget.hasMember(user) && !budget.isOwner(user)) {
            logger.warn("Access denied for user ${currentUser.id} to budget ${budget.id}")
            throw AccessDeniedException("You don't have access to this budget", "Budget", budget.id)
        }
    }

    private fun validateOwnerAccess(budget: Budget) {
        val currentUser = getCurrentUser()
        val user = userRepository.findById(currentUser.id)
            ?: throw EntityNotFoundException("User", currentUser.id)

        if (!budget.isOwner(user)) {
            logger.warn("Owner access denied for user ${currentUser.id} to budget ${budget.id}")
            throw AccessDeniedException("Only the budget owner can perform this action", "Budget", budget.id)
        }
    }

    private fun getCurrentUser(): UserPrincipal {
        val authentication = SecurityContextHolder.getContext().authentication
        return authentication.principal as UserPrincipal
    }
}
