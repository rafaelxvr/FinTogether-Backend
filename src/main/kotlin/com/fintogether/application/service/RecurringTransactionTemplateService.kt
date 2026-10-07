package com.fintogether.application.service

import com.fintogether.application.dto.*
import com.fintogether.application.mapper.RecurringTransactionTemplateMapper
import com.fintogether.domain.entity.RecurringTransactionTemplate
import com.fintogether.domain.exception.AccessDeniedException
import com.fintogether.domain.exception.EntityNotFoundException
import com.fintogether.domain.exception.InvalidOperationException
import com.fintogether.domain.repository.*
import com.fintogether.domain.valueobject.RecurrenceRule
import com.fintogether.infrastructure.security.UserPrincipal
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.UUID

@Service
class RecurringTransactionTemplateService(
    private val templateRepository: RecurringTransactionTemplateRepository,
    private val budgetRepository: BudgetRepository,
    private val userRepository: UserRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val creditCardRepository: CreditCardRepository,
    private val templateMapper: RecurringTransactionTemplateMapper
) {

    private val logger = LoggerFactory.getLogger(RecurringTransactionTemplateService::class.java)

    @Transactional
    fun createTemplate(budgetId: UUID, request: CreateRecurringTemplateRequest): RecurringTemplateResponse {
        logger.info("Creating recurring template for budget: $budgetId, type: ${request.type}, amount: ${request.amount}")

        val budget = budgetRepository.findById(budgetId)
            ?: throw EntityNotFoundException("Budget", budgetId)

        validateBudgetAccess(budget.id)

        val currentUser = getCurrentUser()
        val user = userRepository.findById(currentUser.id)
            ?: throw EntityNotFoundException("User", currentUser.id)

        // Validate recurrence rule
        RecurrenceRule.fromMap(request.recurrenceRule)

        val account = request.accountId?.let { accountId ->
            val acc = accountRepository.findById(accountId)
                ?: throw EntityNotFoundException("Account", accountId)
            if (acc.budget.id != budgetId) {
                throw InvalidOperationException("Account must belong to the same budget", "createTemplate")
            }
            acc
        }

        val category = request.categoryId?.let { categoryId ->
            val cat = categoryRepository.findById(categoryId)
                ?: throw EntityNotFoundException("Category", categoryId)
            if (cat.budget.id != budgetId) {
                throw InvalidOperationException("Category must belong to the same budget", "createTemplate")
            }
            cat
        }

        val card = request.cardId?.let { cardId ->
            val cc = creditCardRepository.findById(cardId)
                ?: throw EntityNotFoundException("CreditCard", cardId)
            if (cc.budget.id != budgetId) {
                throw InvalidOperationException("Credit card must belong to the same budget", "createTemplate")
            }
            cc
        }

        val template = RecurringTransactionTemplate(
            budget = budget,
            user = user,
            amount = request.amount,
            type = request.type,
            account = account,
            category = category,
            card = card,
            description = request.description,
            notes = request.notes,
            tags = request.tags,
            visibility = request.visibility,
            isFixedExpense = request.isFixedExpense,
            recurrenceRule = request.recurrenceRule,
            nextDueDate = request.nextDueDate
        )

        val savedTemplate = templateRepository.save(template)
        logger.info("Recurring template created successfully: ${savedTemplate.id}")
        return templateMapper.toResponse(savedTemplate)
    }

    @Transactional(readOnly = true)
    fun getTemplateById(id: UUID): RecurringTemplateResponse {
        logger.debug("Getting recurring template by id: $id")
        val template = findTemplateById(id)
        validateBudgetAccess(template.budget.id)
        return templateMapper.toResponse(template)
    }

    @Transactional(readOnly = true)
    fun getTemplatesByBudget(budgetId: UUID): List<RecurringTemplateResponse> {
        logger.debug("Getting recurring templates for budget: $budgetId")
        validateBudgetAccess(budgetId)
        val templates = templateRepository.findByBudgetId(budgetId)
        return templates.map { templateMapper.toResponse(it) }
    }

    @Transactional(readOnly = true)
    fun getFixedExpensesByBudget(budgetId: UUID): List<RecurringTemplateResponse> {
        logger.debug("Getting fixed expenses for budget: $budgetId")
        validateBudgetAccess(budgetId)
        val templates = templateRepository.findByBudgetIdAndIsFixedExpense(budgetId, true)
        return templates.map { templateMapper.toResponse(it) }
    }

    @Transactional(readOnly = true)
    fun getFixedExpenseSummary(budgetId: UUID): FixedExpenseSummaryResponse {
        logger.debug("Getting fixed expense summary for budget: $budgetId")
        validateBudgetAccess(budgetId)

        val templates = templateRepository.findByBudgetIdAndIsFixedExpense(budgetId, true)
        val activeTemplates = templates.filter { it.isActive }

        val totalFixedExpenses = activeTemplates.fold(BigDecimal.ZERO) { acc, template ->
            acc.add(template.amount)
        }

        return FixedExpenseSummaryResponse(
            budgetId = budgetId,
            totalFixedExpenses = totalFixedExpenses,
            activeTemplateCount = activeTemplates.size,
            templates = activeTemplates.map { templateMapper.toResponse(it) }
        )
    }

    @Transactional
    fun updateTemplate(id: UUID, request: UpdateRecurringTemplateRequest): RecurringTemplateResponse {
        logger.info("Updating recurring template: $id")
        val template = findTemplateById(id)
        validateBudgetAccess(template.budget.id)

        request.amount?.let { template.amount = it }
        request.type?.let { template.type = it }
        request.description?.let { template.description = it }
        request.notes?.let { template.notes = it }
        request.tags?.let { template.tags = it }
        request.visibility?.let { template.visibility = it }
        request.isFixedExpense?.let { template.isFixedExpense = it }
        request.nextDueDate?.let { template.nextDueDate = it }

        request.recurrenceRule?.let {
            RecurrenceRule.fromMap(it) // Validate
            template.recurrenceRule = it
        }

        request.accountId?.let { accountId ->
            val acc = accountRepository.findById(accountId)
                ?: throw EntityNotFoundException("Account", accountId)
            if (acc.budget.id != template.budget.id) {
                throw InvalidOperationException("Account must belong to the same budget", "updateTemplate")
            }
            template.account = acc
        }

        request.categoryId?.let { categoryId ->
            val cat = categoryRepository.findById(categoryId)
                ?: throw EntityNotFoundException("Category", categoryId)
            if (cat.budget.id != template.budget.id) {
                throw InvalidOperationException("Category must belong to the same budget", "updateTemplate")
            }
            template.category = cat
        }

        request.cardId?.let { cardId ->
            val cc = creditCardRepository.findById(cardId)
                ?: throw EntityNotFoundException("CreditCard", cardId)
            if (cc.budget.id != template.budget.id) {
                throw InvalidOperationException("Credit card must belong to the same budget", "updateTemplate")
            }
            template.card = cc
        }

        val updatedTemplate = templateRepository.save(template)
        logger.info("Recurring template updated successfully: $id")
        return templateMapper.toResponse(updatedTemplate)
    }

    @Transactional
    fun deactivateTemplate(id: UUID): RecurringTemplateResponse {
        logger.info("Deactivating recurring template: $id")
        val template = findTemplateById(id)
        validateBudgetAccess(template.budget.id)

        template.deactivate()
        val updatedTemplate = templateRepository.save(template)
        logger.info("Recurring template deactivated successfully: $id")
        return templateMapper.toResponse(updatedTemplate)
    }

    @Transactional
    fun deleteTemplate(id: UUID) {
        logger.info("Deleting recurring template: $id")
        val template = findTemplateById(id)
        validateBudgetAccess(template.budget.id)

        templateRepository.delete(template)
        logger.info("Recurring template deleted successfully: $id")
    }

    private fun findTemplateById(id: UUID): RecurringTransactionTemplate {
        return templateRepository.findById(id)
            ?: throw EntityNotFoundException("RecurringTransactionTemplate", id)
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
