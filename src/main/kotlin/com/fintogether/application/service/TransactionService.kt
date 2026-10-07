package com.fintogether.application.service

import com.fintogether.application.dto.*
import com.fintogether.application.mapper.TransactionMapper
import com.fintogether.domain.entity.Transaction
import com.fintogether.domain.entity.TransactionType
import com.fintogether.application.dto.PagedResponse
import com.fintogether.domain.exception.AccessDeniedException
import com.fintogether.domain.exception.EntityNotFoundException
import com.fintogether.domain.exception.InvalidOperationException
import com.fintogether.domain.entity.Merchant
import com.fintogether.domain.repository.*
import com.fintogether.infrastructure.security.UserPrincipal
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Service
class TransactionService(
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val userRepository: UserRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val creditCardRepository: CreditCardRepository,
    private val merchantRepository: MerchantRepository,
    private val transactionMapper: TransactionMapper,
    private val transactionSplitService: TransactionSplitService
) {

    private val logger = LoggerFactory.getLogger(TransactionService::class.java)

    @Transactional
    fun createTransaction(budgetId: UUID, request: CreateTransactionRequest): TransactionResponse {
        logger.info("Creating ${request.type} transaction for budget: $budgetId, amount: ${request.amount}")

        val budget = budgetRepository.findById(budgetId)
            ?: throw EntityNotFoundException("Budget", budgetId)

        validateBudgetAccess(budget.id)

        val currentUser = getCurrentUser()
        val user = userRepository.findById(currentUser.id)
            ?: throw EntityNotFoundException("User", currentUser.id)

        // Validate account if provided
        val account = request.accountId?.let { accountId ->
            val acc = accountRepository.findById(accountId)
                ?: throw EntityNotFoundException("Account", accountId)
            if (acc.budget.id != budgetId) {
                logger.warn("Account $accountId belongs to different budget")
                throw InvalidOperationException("Account must belong to the same budget", "createTransaction")
            }
            acc
        }

        // Validate category if provided
        val category = request.categoryId?.let { categoryId ->
            val cat = categoryRepository.findById(categoryId)
                ?: throw EntityNotFoundException("Category", categoryId)
            if (cat.budget.id != budgetId) {
                logger.warn("Category $categoryId belongs to different budget")
                throw InvalidOperationException("Category must belong to the same budget", "createTransaction")
            }
            if (cat.type.name != request.type.name) {
                logger.warn("Category type ${cat.type} doesn't match transaction type ${request.type}")
                throw InvalidOperationException("Category type must match transaction type", "createTransaction")
            }
            cat
        }

        // Validate credit card if provided
        val card = request.cardId?.let { cardId ->
            val cc = creditCardRepository.findById(cardId)
                ?: throw EntityNotFoundException("CreditCard", cardId)
            if (cc.budget.id != budgetId) {
                logger.warn("Credit card $cardId belongs to different budget")
                throw InvalidOperationException("Credit card must belong to the same budget", "createTransaction")
            }
            cc
        }

        // Validate merchant if provided
        val merchant = request.merchantId?.let { merchantId ->
            val m = merchantRepository.findById(merchantId)
                ?: throw EntityNotFoundException("Merchant", merchantId)
            if (m.budget.id != budgetId) {
                logger.warn("Merchant $merchantId belongs to different budget")
                throw InvalidOperationException("Merchant must belong to the same budget", "createTransaction")
            }
            m
        }

        val transaction = Transaction(
            budget = budget,
            user = user,
            transactionDate = request.transactionDate,
            amount = request.amount,
            type = request.type,
            account = account,
            category = category,
            card = card,
            description = request.description,
            notes = request.notes,
            tags = request.tags,
            visibility = request.visibility,
            isRecurring = request.isRecurring,
            recurrenceRule = request.recurrenceRule,
            isInstallment = request.isInstallment,
            installmentGroupId = request.installmentGroupId,
            installmentNumber = request.installmentNumber,
            merchant = merchant
        )

        // Update account balance if account is specified
        if (account != null) {
            when (request.type) {
                TransactionType.INCOME -> account.addAmount(request.amount)
                TransactionType.EXPENSE -> account.subtractAmount(request.amount)
                TransactionType.TRANSFER -> {
                    // Transfer transactions are handled separately
                }
            }
            accountRepository.save(account)
        }

        val savedTransaction = transactionRepository.save(transaction)
        logger.info("Transaction created successfully: ${savedTransaction.id}")

        // Auto-create splits if splitWith is provided
        val splits = if (!request.splitWith.isNullOrEmpty()) {
            transactionSplitService.createSplits(savedTransaction.id, request.splitWith)
        } else {
            null
        }

        return transactionMapper.toResponse(savedTransaction).copy(splits = splits)
    }

    @Transactional(readOnly = true)
    fun getTransactionById(id: UUID): TransactionResponse {
        logger.debug("Getting transaction by id: $id")
        val transaction = findTransactionById(id)
        validateBudgetAccess(transaction.budget.id)
        return transactionMapper.toResponse(transaction)
    }

    @Transactional(readOnly = true)
    fun getTransactionsByBudget(
        budgetId: UUID,
        startDate: LocalDate?,
        endDate: LocalDate?,
        updatedSince: Instant? = null
    ): List<TransactionResponse> {
        logger.debug("Getting transactions for budget: $budgetId, dateRange: $startDate to $endDate, updatedSince: $updatedSince")
        validateBudgetAccess(budgetId)

        // TODO: filter by updatedSince once updatedAt field is added to entities
        val transactions = if (startDate != null && endDate != null) {
            transactionRepository.findByBudgetIdAndDateBetween(budgetId, startDate, endDate)
        } else {
            transactionRepository.findByBudgetId(budgetId)
        }

        return transactions.map { transactionMapper.toResponse(it) }
    }

    @Transactional(readOnly = true)
    fun getTransactionsByBudget(
        budgetId: UUID,
        startDate: LocalDate?,
        endDate: LocalDate?,
        pageable: Pageable
    ): PagedResponse<TransactionResponse> {
        logger.debug("Getting paginated transactions for budget: $budgetId, page: ${pageable.pageNumber}, size: ${pageable.pageSize}")
        validateBudgetAccess(budgetId)

        val page = if (startDate != null && endDate != null) {
            transactionRepository.findByBudgetIdAndDateBetween(budgetId, startDate, endDate, pageable)
        } else {
            transactionRepository.findByBudgetId(budgetId, pageable)
        }

        return PagedResponse.from(page) { transactionMapper.toResponse(it) }
    }

    @Transactional(readOnly = true)
    fun getTransactionsByCategory(categoryId: UUID): List<TransactionResponse> {
        logger.debug("Getting transactions for category: $categoryId")
        val category = categoryRepository.findById(categoryId)
            ?: throw EntityNotFoundException("Category", categoryId)

        validateBudgetAccess(category.budget.id)

        val transactions = transactionRepository.findByCategory(category)
        return transactions.map { transactionMapper.toResponse(it) }
    }

    @Transactional(readOnly = true)
    fun getTransactionsByCard(
        cardId: UUID,
        startDate: LocalDate?,
        endDate: LocalDate?
    ): List<TransactionResponse> {
        logger.debug("Getting transactions for card: $cardId, dateRange: $startDate to $endDate")
        val card = creditCardRepository.findById(cardId)
            ?: throw EntityNotFoundException("CreditCard", cardId)

        validateBudgetAccess(card.budget.id)

        val transactions = if (startDate != null && endDate != null) {
            transactionRepository.findByCardAndDateBetween(card, startDate, endDate)
        } else {
            transactionRepository.findByCard(card)
        }

        return transactions.map { transactionMapper.toResponse(it) }
    }

    @Transactional(readOnly = true)
    fun getTransactionsByInstallmentGroup(groupId: UUID): List<TransactionResponse> {
        logger.debug("Getting transactions for installment group: $groupId")
        val transactions = transactionRepository.findByInstallmentGroupId(groupId)

        if (transactions.isEmpty()) {
            throw EntityNotFoundException("InstallmentGroup", groupId)
        }

        // Validate access to the budget of the first transaction
        validateBudgetAccess(transactions.first().budget.id)

        return transactions.map { transactionMapper.toResponse(it) }
    }

    @Transactional
    fun updateTransaction(id: UUID, request: UpdateTransactionRequest): TransactionResponse {
        logger.info("Updating transaction: $id")
        val transaction = findTransactionById(id)
        validateBudgetAccess(transaction.budget.id)

        // Store old values for balance adjustment
        val oldAmount = transaction.amount
        val oldAccount = transaction.account

        // Update category if provided
        val category = request.categoryId?.let { categoryId ->
            val cat = categoryRepository.findById(categoryId)
                ?: throw EntityNotFoundException("Category", categoryId)
            if (cat.budget.id != transaction.budget.id) {
                logger.warn("Category $categoryId belongs to different budget")
                throw InvalidOperationException("Category must belong to the same budget", "updateTransaction")
            }
            if (cat.type.name != transaction.type.name) {
                logger.warn("Category type ${cat.type} doesn't match transaction type ${transaction.type}")
                throw InvalidOperationException("Category type must match transaction type", "updateTransaction")
            }
            cat
        }

        // Update merchant if provided
        val merchant = request.merchantId?.let { merchantId ->
            val m = merchantRepository.findById(merchantId)
                ?: throw EntityNotFoundException("Merchant", merchantId)
            if (m.budget.id != transaction.budget.id) {
                logger.warn("Merchant $merchantId belongs to different budget")
                throw InvalidOperationException("Merchant must belong to the same budget", "updateTransaction")
            }
            m
        }

        // Update visibility if provided
        request.visibility?.let { transaction.updateVisibility(it) }

        // Update transaction fields
        transaction.update(
            transactionDate = request.transactionDate,
            amount = request.amount,
            description = request.description,
            notes = request.notes,
            tags = request.tags,
            category = category,
            merchant = merchant
        )

        // Adjust account balance if amount changed and account exists
        if (oldAccount != null && oldAmount != request.amount) {
            when (transaction.type) {
                TransactionType.INCOME -> {
                    // Reverse old amount and apply new amount
                    oldAccount.subtractAmount(oldAmount)
                    oldAccount.addAmount(request.amount)
                }
                TransactionType.EXPENSE -> {
                    // Reverse old amount and apply new amount
                    oldAccount.addAmount(oldAmount)
                    oldAccount.subtractAmount(request.amount)
                }
                TransactionType.TRANSFER -> {
                    // Transfer transactions are handled separately
                }
            }
            accountRepository.save(oldAccount)
        }

        val updatedTransaction = transactionRepository.save(transaction)
        logger.info("Transaction updated successfully: $id")
        return transactionMapper.toResponse(updatedTransaction)
    }

    @Transactional
    fun deleteTransaction(id: UUID) {
        logger.info("Deleting transaction: $id")
        val transaction = findTransactionById(id)
        validateBudgetAccess(transaction.budget.id)

        // Reverse the account balance change
        transaction.account?.let { account ->
            when (transaction.type) {
                TransactionType.INCOME -> account.subtractAmount(transaction.amount)
                TransactionType.EXPENSE -> account.addAmount(transaction.amount)
                TransactionType.TRANSFER -> {
                    // Transfer transactions are handled separately
                }
            }
            accountRepository.save(account)
        }

        transactionRepository.delete(transaction)
        logger.info("Transaction deleted successfully: $id")
    }

    @Transactional
    fun bulkCreateTransactions(budgetId: UUID, request: BulkCreateTransactionRequest): BulkTransactionResponse {
        logger.info("Bulk creating ${request.transactions.size} transactions for budget: $budgetId")
        validateBudgetAccess(budgetId)

        val created = mutableListOf<TransactionResponse>()
        val failed = mutableListOf<BulkTransactionError>()

        request.transactions.forEachIndexed { index, transactionRequest ->
            try {
                val transaction = createTransaction(budgetId, transactionRequest)
                created.add(transaction)
            } catch (e: Exception) {
                logger.warn("Failed to create transaction at index $index: ${e.message}")
                failed.add(BulkTransactionError(index = index, error = e.message ?: "Unknown error"))
            }
        }

        logger.info("Bulk create completed: ${created.size} created, ${failed.size} failed")
        return BulkTransactionResponse(created = created, failed = failed)
    }

    @Transactional
    fun deleteTransactionsByInstallmentGroup(groupId: UUID) {
        logger.info("Deleting transactions for installment group: $groupId")
        val transactions = transactionRepository.findByInstallmentGroupId(groupId)

        if (transactions.isEmpty()) {
            throw EntityNotFoundException("InstallmentGroup", groupId)
        }

        validateBudgetAccess(transactions.first().budget.id)

        // Reverse all account balance changes
        transactions.forEach { transaction ->
            transaction.account?.let { account ->
                when (transaction.type) {
                    TransactionType.INCOME -> account.subtractAmount(transaction.amount)
                    TransactionType.EXPENSE -> account.addAmount(transaction.amount)
                    TransactionType.TRANSFER -> {
                        // Transfer transactions are handled separately
                    }
                }
                accountRepository.save(account)
            }
        }

        transactionRepository.deleteAll(transactions)
        logger.info("Deleted ${transactions.size} transactions for installment group: $groupId")
    }

    @Transactional(readOnly = true)
    fun searchTransactions(
        budgetId: UUID,
        query: String?,
        type: TransactionType?,
        categoryId: UUID?,
        merchantId: UUID?,
        minAmount: BigDecimal?,
        maxAmount: BigDecimal?,
        startDate: LocalDate?,
        endDate: LocalDate?,
        pageable: Pageable
    ): PagedResponse<TransactionResponse> {
        logger.debug("Searching transactions for budget: $budgetId, query: $query")
        validateBudgetAccess(budgetId)

        val trimmedQuery = query?.trim()?.takeIf { it.isNotEmpty() }

        val results = transactionRepository.search(
            budgetId = budgetId,
            query = trimmedQuery,
            type = type,
            categoryId = categoryId,
            merchantId = merchantId,
            minAmount = minAmount,
            maxAmount = maxAmount,
            startDate = startDate,
            endDate = endDate,
            pageable = pageable
        )

        return PagedResponse.from(results) { transactionMapper.toResponse(it) }
    }

    private fun findTransactionById(id: UUID): Transaction {
        return transactionRepository.findById(id)
            ?: throw EntityNotFoundException("Transaction", id)
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
