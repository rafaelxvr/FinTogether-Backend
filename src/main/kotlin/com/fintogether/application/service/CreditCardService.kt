package com.fintogether.application.service

import com.fintogether.application.dto.*
import com.fintogether.domain.entity.CreditCard
import com.fintogether.application.dto.PagedResponse
import com.fintogether.domain.exception.AccessDeniedException
import com.fintogether.domain.exception.EntityNotFoundException
import com.fintogether.domain.exception.InvalidOperationException
import com.fintogether.domain.repository.AccountRepository
import com.fintogether.domain.repository.BudgetRepository
import com.fintogether.domain.repository.CreditCardRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

/**
 * Credit Card Service
 * Handles business logic for credit card management
 */
@Service
@Transactional
class CreditCardService(
    private val creditCardRepository: CreditCardRepository,
    private val budgetRepository: BudgetRepository,
    private val accountRepository: AccountRepository
) {

    private val logger = LoggerFactory.getLogger(CreditCardService::class.java)

    /**
     * Create a new credit card
     */
    fun createCreditCard(request: CreateCreditCardRequest, userId: UUID): CreditCardResponse {
        logger.info("Creating credit card '${request.name}' for user: $userId")

        val budgetId = UUID.fromString(request.budgetId)
        val budget = budgetRepository.findById(budgetId)
            ?: throw EntityNotFoundException("Budget", budgetId)

        // Verify user has access to budget
        validateBudgetMembership(budget, userId)

        // Validate and get linked account if provided
        val linkedAccount = request.linkedAccountId?.let { accountIdStr ->
            val accountId = UUID.fromString(accountIdStr)
            val account = accountRepository.findById(accountId)
                ?: throw EntityNotFoundException("Account", accountId)

            // Verify account belongs to same budget
            if (account.budget.id != budget.id) {
                logger.warn("Account $accountId belongs to different budget")
                throw InvalidOperationException("Account must belong to the same budget", "createCreditCard")
            }

            account
        }

        val creditCard = CreditCard(
            budget = budget,
            name = request.name,
            issuer = request.issuer,
            lastFour = request.lastFour,
            currency = request.currency,
            statementCloseDay = request.statementCloseDay,
            dueDay = request.dueDay,
            linkedAccount = linkedAccount,
            creditLimit = request.creditLimit
        )

        val saved = creditCardRepository.save(creditCard)
        logger.info("Credit card created successfully: ${saved.id}")
        return CreditCardResponse.from(saved)
    }

    /**
     * Get credit card by ID
     */
    fun getCreditCardById(id: UUID, userId: UUID): CreditCardResponse {
        logger.debug("Getting credit card by id: $id")
        val creditCard = creditCardRepository.findById(id)
            ?: throw EntityNotFoundException("CreditCard", id)

        validateBudgetMembership(creditCard.budget, userId)

        return CreditCardResponse.from(creditCard)
    }

    /**
     * Get all credit cards for a budget
     */
    fun getCreditCardsByBudget(budgetId: UUID, userId: UUID, updatedSince: Instant? = null): List<CreditCardResponse> {
        logger.debug("Getting credit cards for budget: $budgetId, updatedSince: $updatedSince")
        val budget = budgetRepository.findById(budgetId)
            ?: throw EntityNotFoundException("Budget", budgetId)

        validateBudgetMembership(budget, userId)

        // TODO: filter by updatedSince once updatedAt field is added to entities
        val cards = creditCardRepository.findByBudgetId(budgetId)
        return cards.map { CreditCardResponse.from(it) }
    }

    /**
     * Get paginated credit cards for a budget
     */
    fun getCreditCardsByBudget(budgetId: UUID, userId: UUID, pageable: Pageable): PagedResponse<CreditCardResponse> {
        logger.debug("Getting paginated credit cards for budget: $budgetId, page: ${pageable.pageNumber}, size: ${pageable.pageSize}")
        val budget = budgetRepository.findById(budgetId)
            ?: throw EntityNotFoundException("Budget", budgetId)

        validateBudgetMembership(budget, userId)

        val page = creditCardRepository.findByBudgetId(budgetId, pageable)
        return PagedResponse.from(page) { CreditCardResponse.from(it) }
    }

    /**
     * Get only active credit cards for a budget
     */
    fun getActiveCreditCards(budgetId: UUID, userId: UUID): List<CreditCardResponse> {
        logger.debug("Getting active credit cards for budget: $budgetId")
        val budget = budgetRepository.findById(budgetId)
            ?: throw EntityNotFoundException("Budget", budgetId)

        validateBudgetMembership(budget, userId)

        return creditCardRepository.findByBudgetAndActive(budget, true)
            .map { CreditCardResponse.from(it) }
    }

    /**
     * Update credit card
     */
    fun updateCreditCard(id: UUID, request: UpdateCreditCardRequest, userId: UUID): CreditCardResponse {
        logger.info("Updating credit card: $id")
        val creditCard = creditCardRepository.findById(id)
            ?: throw EntityNotFoundException("CreditCard", id)

        validateBudgetMembership(creditCard.budget, userId)

        // Validate and get linked account if provided
        val linkedAccount = request.linkedAccountId?.let { accountIdStr ->
            val accountId = UUID.fromString(accountIdStr)
            val account = accountRepository.findById(accountId)
                ?: throw EntityNotFoundException("Account", accountId)

            // Verify account belongs to same budget
            if (account.budget.id != creditCard.budget.id) {
                logger.warn("Account $accountId belongs to different budget")
                throw InvalidOperationException("Account must belong to the same budget", "updateCreditCard")
            }

            account
        }

        creditCard.update(
            name = request.name,
            issuer = request.issuer,
            lastFour = request.lastFour,
            currency = request.currency,
            statementCloseDay = request.statementCloseDay,
            dueDay = request.dueDay,
            linkedAccount = linkedAccount,
            creditLimit = request.creditLimit
        )

        val saved = creditCardRepository.save(creditCard)
        logger.info("Credit card updated successfully: $id")
        return CreditCardResponse.from(saved)
    }

    /**
     * Delete credit card
     */
    fun deleteCreditCard(id: UUID, userId: UUID) {
        logger.info("Deleting credit card: $id")
        val creditCard = creditCardRepository.findById(id)
            ?: throw EntityNotFoundException("CreditCard", id)

        validateBudgetMembership(creditCard.budget, userId)

        creditCardRepository.delete(creditCard)
        logger.info("Credit card deleted successfully: $id")
    }

    /**
     * Toggle credit card active status
     */
    fun toggleStatus(id: UUID, request: ToggleCreditCardStatusRequest, userId: UUID): CreditCardResponse {
        logger.info("Toggling credit card status: $id, active: ${request.active}")
        val creditCard = creditCardRepository.findById(id)
            ?: throw EntityNotFoundException("CreditCard", id)

        validateBudgetMembership(creditCard.budget, userId)

        if (request.active) {
            creditCard.activate()
        } else {
            creditCard.deactivate()
        }

        val saved = creditCardRepository.save(creditCard)
        logger.info("Credit card status toggled successfully: $id")
        return CreditCardResponse.from(saved)
    }

    /**
     * Link an account to credit card (for payments)
     */
    fun linkAccount(id: UUID, request: LinkAccountRequest, userId: UUID): CreditCardResponse {
        logger.info("Linking account ${request.accountId} to credit card: $id")
        val creditCard = creditCardRepository.findById(id)
            ?: throw EntityNotFoundException("CreditCard", id)

        validateBudgetMembership(creditCard.budget, userId)

        val accountId = UUID.fromString(request.accountId)
        val account = accountRepository.findById(accountId)
            ?: throw EntityNotFoundException("Account", accountId)

        // Verify account belongs to same budget
        if (account.budget.id != creditCard.budget.id) {
            logger.warn("Account $accountId belongs to different budget")
            throw InvalidOperationException("Account must belong to the same budget", "linkAccount")
        }

        creditCard.linkAccount(account)

        val saved = creditCardRepository.save(creditCard)
        logger.info("Account linked successfully to credit card: $id")
        return CreditCardResponse.from(saved)
    }

    /**
     * Unlink account from credit card
     */
    fun unlinkAccount(id: UUID, userId: UUID): CreditCardResponse {
        logger.info("Unlinking account from credit card: $id")
        val creditCard = creditCardRepository.findById(id)
            ?: throw EntityNotFoundException("CreditCard", id)

        validateBudgetMembership(creditCard.budget, userId)

        creditCard.unlinkAccount()

        val saved = creditCardRepository.save(creditCard)
        logger.info("Account unlinked successfully from credit card: $id")
        return CreditCardResponse.from(saved)
    }

    /**
     * Validate that user is a member of the budget
     */
    private fun validateBudgetMembership(budget: com.fintogether.domain.entity.Budget, userId: UUID) {
        val isMember = budget.members.any { it.user.id == userId }
        if (!isMember) {
            logger.warn("Access denied for user $userId to budget ${budget.id}")
            throw AccessDeniedException("User is not a member of this budget", "Budget", budget.id)
        }
    }
}
