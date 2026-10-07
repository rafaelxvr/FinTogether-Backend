package com.fintogether.application.service

import com.fintogether.application.dto.CreateInstallmentGroupRequest
import com.fintogether.application.dto.InstallmentGroupProgressResponse
import com.fintogether.application.dto.InstallmentGroupResponse
import com.fintogether.application.mapper.InstallmentGroupMapper
import com.fintogether.domain.entity.InstallmentGroup
import com.fintogether.domain.entity.InstallmentGroupStatus
import com.fintogether.domain.entity.Transaction
import com.fintogether.domain.entity.TransactionType
import com.fintogether.domain.entity.TransactionVisibility
import com.fintogether.domain.exception.AccessDeniedException
import com.fintogether.domain.exception.EntityNotFoundException
import com.fintogether.domain.exception.InvalidOperationException
import com.fintogether.domain.repository.*
import com.fintogether.infrastructure.security.UserPrincipal
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class InstallmentGroupService(
    private val installmentGroupRepository: InstallmentGroupRepository,
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val userRepository: UserRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val creditCardRepository: CreditCardRepository,
    private val installmentGroupMapper: InstallmentGroupMapper
) {

    private val logger = LoggerFactory.getLogger(InstallmentGroupService::class.java)

    @Transactional
    fun createInstallmentGroup(
        budgetId: UUID,
        request: CreateInstallmentGroupRequest,
        userId: UUID
    ): InstallmentGroupResponse {
        logger.info("Creating installment group for budget: $budgetId, amount: ${request.totalAmount}, count: ${request.installmentCount}")

        val budget = budgetRepository.findById(budgetId)
            ?: throw EntityNotFoundException("Budget", budgetId)

        validateBudgetAccess(budgetId)

        val currentUser = getCurrentUser()
        val user = userRepository.findById(currentUser.id)
            ?: throw EntityNotFoundException("User", currentUser.id)

        // Validate account if provided
        val account = request.accountId?.let { accountId ->
            val acc = accountRepository.findById(accountId)
                ?: throw EntityNotFoundException("Account", accountId)
            if (acc.budget.id != budgetId) {
                throw InvalidOperationException("Account must belong to the same budget", "createInstallmentGroup")
            }
            acc
        }

        // Validate category if provided
        val category = request.categoryId?.let { categoryId ->
            val cat = categoryRepository.findById(categoryId)
                ?: throw EntityNotFoundException("Category", categoryId)
            if (cat.budget.id != budgetId) {
                throw InvalidOperationException("Category must belong to the same budget", "createInstallmentGroup")
            }
            cat
        }

        // Validate credit card if provided
        val card = request.cardId?.let { cardId ->
            val cc = creditCardRepository.findById(cardId)
                ?: throw EntityNotFoundException("CreditCard", cardId)
            if (cc.budget.id != budgetId) {
                throw InvalidOperationException("Credit card must belong to the same budget", "createInstallmentGroup")
            }
            cc
        }

        val installmentGroup = InstallmentGroup(
            budget = budget,
            card = card,
            totalAmount = request.totalAmount,
            installmentCount = request.installmentCount,
            interestRate = request.interestRate,
            startDate = request.startDate,
            description = request.description
        )

        val savedGroup = installmentGroupRepository.save(installmentGroup)
        val installmentAmount = savedGroup.calculateInstallmentAmount()
        val visibility = TransactionVisibility.valueOf(request.visibility)

        // Generate N transactions, one per month
        val transactions = (1..request.installmentCount).map { number ->
            val transactionDate = request.startDate.plusMonths((number - 1).toLong())
            val installmentDescription = buildInstallmentDescription(request.description, number, request.installmentCount)

            Transaction(
                budget = budget,
                user = user,
                transactionDate = transactionDate,
                amount = installmentAmount,
                type = TransactionType.EXPENSE,
                account = account,
                category = category,
                card = card,
                description = installmentDescription,
                visibility = visibility,
                isInstallment = true,
                installmentGroupId = savedGroup.id,
                installmentNumber = number
            )
        }

        transactionRepository.saveAll(transactions)
        logger.info("Installment group created: ${savedGroup.id} with ${transactions.size} transactions")

        return installmentGroupMapper.toResponse(savedGroup)
    }

    @Transactional(readOnly = true)
    fun getInstallmentGroupProgress(groupId: UUID, userId: UUID): InstallmentGroupProgressResponse {
        logger.debug("Getting installment group progress: $groupId")

        val group = installmentGroupRepository.findById(groupId)
            ?: throw EntityNotFoundException("InstallmentGroup", groupId)

        validateBudgetAccess(group.budget.id)

        val transactions = transactionRepository.findByInstallmentGroupId(groupId)

        return installmentGroupMapper.toProgressResponse(group, transactions)
    }

    @Transactional(readOnly = true)
    fun getInstallmentGroupsByBudget(budgetId: UUID, userId: UUID): List<InstallmentGroupResponse> {
        logger.debug("Getting installment groups for budget: $budgetId")

        validateBudgetAccess(budgetId)

        val groups = installmentGroupRepository.findByBudgetId(budgetId)
        return groups.map { installmentGroupMapper.toResponse(it) }
    }

    @Transactional
    fun deleteInstallmentGroup(groupId: UUID, userId: UUID) {
        logger.info("Deleting installment group: $groupId")

        val group = installmentGroupRepository.findById(groupId)
            ?: throw EntityNotFoundException("InstallmentGroup", groupId)

        validateBudgetAccess(group.budget.id)

        // Delete associated transactions first
        val transactions = transactionRepository.findByInstallmentGroupId(groupId)

        // Reverse account balance changes
        transactions.forEach { transaction ->
            transaction.account?.let { account ->
                when (transaction.type) {
                    TransactionType.INCOME -> account.subtractAmount(transaction.amount)
                    TransactionType.EXPENSE -> account.addAmount(transaction.amount)
                    TransactionType.TRANSFER -> { /* handled separately */ }
                }
                accountRepository.save(account)
            }
        }

        transactionRepository.deleteAll(transactions)
        installmentGroupRepository.delete(group)

        logger.info("Installment group deleted: $groupId with ${transactions.size} transactions")
    }

    private fun buildInstallmentDescription(description: String?, number: Int, total: Int): String {
        val base = description ?: "Installment"
        return "$base ($number/$total)"
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
