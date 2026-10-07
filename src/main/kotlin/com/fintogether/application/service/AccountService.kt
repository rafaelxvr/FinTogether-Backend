package com.fintogether.application.service

import com.fintogether.application.dto.AccountResponse
import com.fintogether.application.dto.CreateAccountRequest
import com.fintogether.application.dto.TransferRequest
import com.fintogether.application.dto.UpdateAccountRequest
import com.fintogether.application.mapper.AccountMapper
import com.fintogether.domain.entity.Account
import com.fintogether.application.dto.PagedResponse
import com.fintogether.domain.exception.AccessDeniedException
import com.fintogether.domain.exception.EntityNotFoundException
import com.fintogether.domain.exception.InvalidOperationException
import com.fintogether.domain.repository.AccountRepository
import com.fintogether.domain.repository.BudgetRepository
import com.fintogether.domain.repository.UserRepository
import com.fintogether.infrastructure.security.UserPrincipal
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Service
class AccountService(
    private val accountRepository: AccountRepository,
    private val budgetRepository: BudgetRepository,
    private val userRepository: UserRepository,
    private val accountMapper: AccountMapper
) {

    private val logger = LoggerFactory.getLogger(AccountService::class.java)

    @Transactional
    fun createAccount(budgetId: UUID, request: CreateAccountRequest): AccountResponse {
        logger.info("Creating account '${request.name}' for budget: $budgetId")

        val budget = budgetRepository.findById(budgetId)
            ?: throw EntityNotFoundException("Budget", budgetId)

        validateBudgetAccess(budget.id)

        val account = Account(
            budget = budget,
            name = request.name,
            type = request.type,
            currency = request.currency,
            openingBalance = request.openingBalance,
            currentBalance = request.openingBalance
        )

        val savedAccount = accountRepository.save(account)
        logger.info("Account created successfully: ${savedAccount.id}")
        return accountMapper.toResponse(savedAccount)
    }

    @Transactional(readOnly = true)
    fun getAccountById(id: UUID): AccountResponse {
        logger.debug("Getting account by id: $id")
        val account = findAccountById(id)
        validateBudgetAccess(account.budget.id)
        return accountMapper.toResponse(account)
    }

    @Transactional(readOnly = true)
    fun getAccountsByBudget(budgetId: UUID, updatedSince: Instant? = null): List<AccountResponse> {
        logger.debug("Getting accounts for budget: $budgetId, updatedSince: $updatedSince")
        validateBudgetAccess(budgetId)
        // TODO: filter by updatedSince once updatedAt field is added to entities
        val accounts = accountRepository.findByBudgetId(budgetId)
        return accounts.map { accountMapper.toResponse(it) }
    }

    @Transactional(readOnly = true)
    fun getAccountsByBudget(budgetId: UUID, pageable: Pageable): PagedResponse<AccountResponse> {
        logger.debug("Getting paginated accounts for budget: $budgetId, page: ${pageable.pageNumber}, size: ${pageable.pageSize}")
        validateBudgetAccess(budgetId)
        val page = accountRepository.findByBudgetId(budgetId, pageable)
        return PagedResponse.from(page) { accountMapper.toResponse(it) }
    }

    @Transactional
    fun updateAccount(id: UUID, request: UpdateAccountRequest): AccountResponse {
        logger.info("Updating account: $id")
        val account = findAccountById(id)
        validateBudgetAccess(account.budget.id)

        account.update(name = request.name, currency = request.currency)

        val updatedAccount = accountRepository.save(account)
        logger.info("Account updated successfully: $id")
        return accountMapper.toResponse(updatedAccount)
    }

    @Transactional
    fun deleteAccount(id: UUID) {
        logger.info("Deleting account: $id")
        val account = findAccountById(id)
        validateBudgetAccess(account.budget.id)

        if (account.currentBalance != BigDecimal.ZERO) {
            logger.warn("Cannot delete account $id with non-zero balance: ${account.currentBalance}")
            throw InvalidOperationException("Cannot delete account with non-zero balance", "deleteAccount")
        }

        accountRepository.delete(account)
        logger.info("Account deleted successfully: $id")
    }

    @Transactional
    fun transfer(request: TransferRequest) {
        logger.info("Transferring ${request.amount} from account ${request.fromAccountId} to ${request.toAccountId}")

        val fromAccount = findAccountById(request.fromAccountId)
        val toAccount = findAccountById(request.toAccountId)

        validateBudgetAccess(fromAccount.budget.id)
        validateBudgetAccess(toAccount.budget.id)

        if (request.amount <= BigDecimal.ZERO) {
            logger.warn("Transfer amount must be positive: ${request.amount}")
            throw InvalidOperationException("Transfer amount must be positive", "transfer")
        }

        if (fromAccount.currentBalance < request.amount) {
            logger.warn("Insufficient balance in source account ${request.fromAccountId}: ${fromAccount.currentBalance} < ${request.amount}")
            throw InvalidOperationException("Insufficient balance in source account", "transfer")
        }

        fromAccount.subtractAmount(request.amount)
        toAccount.addAmount(request.amount)

        accountRepository.save(fromAccount)
        accountRepository.save(toAccount)
        logger.info("Transfer completed successfully")
    }

    @Transactional
    fun deactivateAccount(id: UUID): AccountResponse {
        logger.info("Deactivating account: $id")
        val account = findAccountById(id)
        validateBudgetAccess(account.budget.id)

        account.deactivate()
        val updatedAccount = accountRepository.save(account)
        logger.info("Account deactivated successfully: $id")
        return accountMapper.toResponse(updatedAccount)
    }

    @Transactional
    fun activateAccount(id: UUID): AccountResponse {
        logger.info("Activating account: $id")
        val account = findAccountById(id)
        validateBudgetAccess(account.budget.id)

        account.activate()
        val updatedAccount = accountRepository.save(account)
        logger.info("Account activated successfully: $id")
        return accountMapper.toResponse(updatedAccount)
    }

    private fun findAccountById(id: UUID): Account {
        return accountRepository.findById(id)
            ?: throw EntityNotFoundException("Account", id)
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
