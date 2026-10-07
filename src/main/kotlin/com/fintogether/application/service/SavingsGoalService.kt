package com.fintogether.application.service

import com.fintogether.application.dto.*
import com.fintogether.application.mapper.SavingsGoalMapper
import com.fintogether.domain.entity.AccountType
import com.fintogether.domain.entity.SavingsGoal
import com.fintogether.domain.entity.SavingsGoalContribution
import com.fintogether.domain.exception.AccessDeniedException
import com.fintogether.domain.exception.EntityNotFoundException
import com.fintogether.domain.exception.InvalidOperationException
import com.fintogether.domain.repository.AccountRepository
import com.fintogether.domain.repository.BudgetRepository
import com.fintogether.domain.repository.SavingsGoalContributionRepository
import com.fintogether.domain.repository.SavingsGoalRepository
import com.fintogether.domain.repository.UserRepository
import com.fintogether.infrastructure.security.UserPrincipal
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.UUID

@Service
class SavingsGoalService(
    private val savingsGoalRepository: SavingsGoalRepository,
    private val contributionRepository: SavingsGoalContributionRepository,
    private val budgetRepository: BudgetRepository,
    private val accountRepository: AccountRepository,
    private val userRepository: UserRepository,
    private val savingsGoalMapper: SavingsGoalMapper
) {

    private val logger = LoggerFactory.getLogger(SavingsGoalService::class.java)

    @Transactional
    fun createSavingsGoal(budgetId: UUID, request: CreateSavingsGoalRequest): SavingsGoalResponse {
        logger.info("Creating savings goal '${request.name}' for budget: $budgetId")

        val budget = budgetRepository.findById(budgetId)
            ?: throw EntityNotFoundException("Budget", budgetId)

        validateBudgetAccess(budget.id)

        val account = request.accountId?.let { accountId ->
            val acc = accountRepository.findById(accountId)
                ?: throw EntityNotFoundException("Account", accountId)
            if (acc.budget.id != budgetId) {
                throw InvalidOperationException(
                    "Account does not belong to the same budget",
                    "createSavingsGoal"
                )
            }
            if (acc.type != AccountType.SAVINGS) {
                throw InvalidOperationException(
                    "Linked account must be of type SAVINGS",
                    "createSavingsGoal"
                )
            }
            acc
        }

        val savingsGoal = SavingsGoal(
            budget = budget,
            name = request.name,
            targetAmount = request.targetAmount,
            currency = request.currency,
            description = request.description,
            deadline = request.deadline,
            icon = request.icon,
            color = request.color,
            account = account
        )

        val savedGoal = savingsGoalRepository.save(savingsGoal)
        logger.info("Savings goal created successfully: ${savedGoal.id}")
        return savingsGoalMapper.toResponse(savedGoal)
    }

    @Transactional(readOnly = true)
    fun getSavingsGoalById(id: UUID): SavingsGoalResponse {
        logger.debug("Getting savings goal by id: $id")
        val savingsGoal = findSavingsGoalById(id)
        validateBudgetAccess(savingsGoal.budget.id)
        return savingsGoalMapper.toResponse(savingsGoal)
    }

    @Transactional(readOnly = true)
    fun getSavingsGoalsByBudget(budgetId: UUID): List<SavingsGoalResponse> {
        logger.debug("Getting savings goals for budget: $budgetId")
        validateBudgetAccess(budgetId)
        val goals = savingsGoalRepository.findByBudgetId(budgetId)
        return goals.map { savingsGoalMapper.toResponse(it) }
    }

    @Transactional(readOnly = true)
    fun getSavingsGoalsByBudget(budgetId: UUID, pageable: Pageable): PagedResponse<SavingsGoalResponse> {
        logger.debug("Getting paginated savings goals for budget: $budgetId")
        validateBudgetAccess(budgetId)
        val page = savingsGoalRepository.findByBudgetId(budgetId, pageable)
        return PagedResponse.from(page) { savingsGoalMapper.toResponse(it) }
    }

    @Transactional
    fun updateSavingsGoal(id: UUID, request: UpdateSavingsGoalRequest): SavingsGoalResponse {
        logger.info("Updating savings goal: $id")
        val savingsGoal = findSavingsGoalById(id)
        validateBudgetAccess(savingsGoal.budget.id)

        savingsGoal.update(
            name = request.name,
            description = request.description,
            targetAmount = request.targetAmount,
            deadline = request.deadline,
            icon = request.icon,
            color = request.color
        )

        val updatedGoal = savingsGoalRepository.save(savingsGoal)
        logger.info("Savings goal updated successfully: $id")
        return savingsGoalMapper.toResponse(updatedGoal)
    }

    @Transactional
    fun deleteSavingsGoal(id: UUID) {
        logger.info("Deleting savings goal: $id")
        val savingsGoal = findSavingsGoalById(id)
        validateBudgetAccess(savingsGoal.budget.id)

        if (savingsGoal.currentAmount != BigDecimal.ZERO) {
            logger.warn("Cannot delete savings goal $id with non-zero balance: ${savingsGoal.currentAmount}")
            throw InvalidOperationException(
                "Cannot delete savings goal with non-zero balance. Withdraw all funds first.",
                "deleteSavingsGoal"
            )
        }

        savingsGoalRepository.delete(savingsGoal)
        logger.info("Savings goal deleted successfully: $id")
    }

    @Transactional
    fun contribute(id: UUID, request: ContributeRequest): SavingsGoalResponse {
        logger.info("Contributing ${request.amount} to savings goal: $id")
        val savingsGoal = findSavingsGoalById(id)
        validateBudgetAccess(savingsGoal.budget.id)

        savingsGoal.contribute(request.amount)

        val contribution = SavingsGoalContribution(
            savingsGoal = savingsGoal,
            amount = request.amount,
            note = request.note
        )
        contributionRepository.save(contribution)

        savingsGoal.account?.let { account ->
            account.addAmount(request.amount)
            accountRepository.save(account)
        }

        val updatedGoal = savingsGoalRepository.save(savingsGoal)
        logger.info("Contribution recorded successfully for savings goal: $id")
        return savingsGoalMapper.toResponse(updatedGoal)
    }

    @Transactional
    fun withdraw(id: UUID, request: WithdrawRequest): SavingsGoalResponse {
        logger.info("Withdrawing ${request.amount} from savings goal: $id")
        val savingsGoal = findSavingsGoalById(id)
        validateBudgetAccess(savingsGoal.budget.id)

        savingsGoal.withdraw(request.amount)

        val contribution = SavingsGoalContribution(
            savingsGoal = savingsGoal,
            amount = request.amount.negate(),
            note = request.note
        )
        contributionRepository.save(contribution)

        savingsGoal.account?.let { account ->
            account.subtractAmount(request.amount)
            accountRepository.save(account)
        }

        val updatedGoal = savingsGoalRepository.save(savingsGoal)
        logger.info("Withdrawal recorded successfully for savings goal: $id")
        return savingsGoalMapper.toResponse(updatedGoal)
    }

    @Transactional(readOnly = true)
    fun getContributions(id: UUID): List<ContributionResponse> {
        logger.debug("Getting contributions for savings goal: $id")
        val savingsGoal = findSavingsGoalById(id)
        validateBudgetAccess(savingsGoal.budget.id)
        val contributions = contributionRepository.findBySavingsGoalId(id)
        return contributions.map { savingsGoalMapper.toContributionResponse(it) }
    }

    private fun findSavingsGoalById(id: UUID): SavingsGoal {
        return savingsGoalRepository.findById(id)
            ?: throw EntityNotFoundException("SavingsGoal", id)
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
