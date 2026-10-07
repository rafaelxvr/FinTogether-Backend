package com.fintogether.application.service

import com.fintogether.application.dto.*
import com.fintogether.application.mapper.TransactionSplitMapper
import com.fintogether.domain.entity.TransactionSplit
import com.fintogether.domain.exception.AccessDeniedException
import com.fintogether.domain.exception.EntityNotFoundException
import com.fintogether.domain.exception.InvalidOperationException
import com.fintogether.domain.repository.*
import com.fintogether.infrastructure.security.UserPrincipal
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

@Service
class TransactionSplitService(
    private val transactionSplitRepository: TransactionSplitRepository,
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val userRepository: UserRepository,
    private val transactionSplitMapper: TransactionSplitMapper
) {

    private val logger = LoggerFactory.getLogger(TransactionSplitService::class.java)

    @Transactional
    fun createSplits(transactionId: UUID, splits: List<CreateSplitRequest>): List<SplitResponse> {
        logger.info("Creating ${splits.size} splits for transaction: $transactionId")

        val transaction = transactionRepository.findById(transactionId)
            ?: throw EntityNotFoundException("Transaction", transactionId)

        validateBudgetAccess(transaction.budget.id)

        val budget = transaction.budget

        // Validate all users are budget members
        splits.forEach { splitRequest ->
            val user = userRepository.findById(splitRequest.userId)
                ?: throw EntityNotFoundException("User", splitRequest.userId)
            if (!budget.hasMember(user) && !budget.isOwner(user)) {
                throw InvalidOperationException(
                    "User ${splitRequest.userId} is not a member of this budget",
                    "createSplits"
                )
            }
        }

        // Validate split amounts sum equals transaction amount
        val splitSum = splits.fold(BigDecimal.ZERO) { acc, s -> acc.add(s.amount) }
        if (splitSum.setScale(2, RoundingMode.HALF_UP).compareTo(
                transaction.amount.setScale(2, RoundingMode.HALF_UP)
            ) != 0
        ) {
            throw InvalidOperationException(
                "Split amounts ($splitSum) must equal transaction amount (${transaction.amount})",
                "createSplits"
            )
        }

        val savedSplits = splits.map { splitRequest ->
            val user = userRepository.findById(splitRequest.userId)!!
            val split = TransactionSplit(
                transaction = transaction,
                user = user,
                amount = splitRequest.amount
            )
            transactionSplitRepository.save(split)
        }

        logger.info("Created ${savedSplits.size} splits for transaction: $transactionId")
        return savedSplits.map { transactionSplitMapper.toResponse(it) }
    }

    @Transactional(readOnly = true)
    fun getSplitsForTransaction(transactionId: UUID): List<SplitResponse> {
        logger.debug("Getting splits for transaction: $transactionId")
        val transaction = transactionRepository.findById(transactionId)
            ?: throw EntityNotFoundException("Transaction", transactionId)
        validateBudgetAccess(transaction.budget.id)

        val splits = transactionSplitRepository.findByTransactionId(transactionId)
        return splits.map { transactionSplitMapper.toResponse(it) }
    }

    @Transactional(readOnly = true)
    fun getBalances(budgetId: UUID): BalanceSummaryResponse {
        logger.debug("Calculating balances for budget: $budgetId")
        validateBudgetAccess(budgetId)

        val allSplits = transactionSplitRepository.findByBudgetId(budgetId)
        val unsettledSplits = allSplits.filter { !it.isPaid }

        // Build net debt map: Map<Pair<debtorId, creditorId>, BigDecimal>
        // The creditor is the person who paid the transaction (transaction.user)
        val debts = mutableMapOf<Pair<UUID, UUID>, BigDecimal>()

        for (split in unsettledSplits) {
            val creditorId = split.transaction.user?.id ?: continue
            val debtorId = split.user.id

            // Skip if the debtor is also the creditor (they paid their own share)
            if (debtorId == creditorId) continue

            val key = Pair(debtorId, creditorId)
            debts[key] = (debts[key] ?: BigDecimal.ZERO).add(split.amount)
        }

        // Simplify: net off A->B and B->A
        val simplifiedEntries = mutableListOf<SplitBalanceEntry>()
        val processed = mutableSetOf<Pair<UUID, UUID>>()

        for ((key, amount) in debts) {
            if (processed.contains(key)) continue

            val (debtorId, creditorId) = key
            val reverseKey = Pair(creditorId, debtorId)
            val reverseAmount = debts[reverseKey] ?: BigDecimal.ZERO

            processed.add(key)
            processed.add(reverseKey)

            val netAmount = amount.subtract(reverseAmount)
            if (netAmount.compareTo(BigDecimal.ZERO) > 0) {
                val debtor = userRepository.findById(debtorId)
                val creditor = userRepository.findById(creditorId)
                if (debtor != null && creditor != null) {
                    simplifiedEntries.add(
                        SplitBalanceEntry(
                            userId = debtorId,
                            userName = debtor.name,
                            owesTo = creditorId,
                            owesToName = creditor.name,
                            netAmount = netAmount.setScale(2, RoundingMode.HALF_UP)
                        )
                    )
                }
            } else if (netAmount.compareTo(BigDecimal.ZERO) < 0) {
                val debtor = userRepository.findById(creditorId)
                val creditor = userRepository.findById(debtorId)
                if (debtor != null && creditor != null) {
                    simplifiedEntries.add(
                        SplitBalanceEntry(
                            userId = creditorId,
                            userName = debtor.name,
                            owesTo = debtorId,
                            owesToName = creditor.name,
                            netAmount = netAmount.abs().setScale(2, RoundingMode.HALF_UP)
                        )
                    )
                }
            }
            // If netAmount == 0, they cancel out, no entry needed
        }

        return BalanceSummaryResponse(budgetId = budgetId, entries = simplifiedEntries)
    }

    @Transactional
    fun settleSplit(splitId: UUID): SplitResponse {
        logger.info("Settling split: $splitId")

        val split = transactionSplitRepository.findById(splitId)
            ?: throw EntityNotFoundException("TransactionSplit", splitId)

        validateBudgetAccess(split.transaction.budget.id)

        split.settle()
        val savedSplit = transactionSplitRepository.save(split)

        logger.info("Split settled: $splitId")
        return transactionSplitMapper.toResponse(savedSplit)
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
