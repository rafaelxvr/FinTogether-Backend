package com.fintogether.application.service

import com.fintogether.application.dto.*
import com.fintogether.domain.exception.AccessDeniedException
import com.fintogether.domain.exception.EntityNotFoundException
import com.fintogether.domain.repository.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.*

/**
 * Credit Card Cost Service
 * Provides billing-cycle-aware cost aggregation per card.
 */
@Service
@Transactional(readOnly = true)
class CreditCardCostService(
    private val creditCardRepository: CreditCardRepository,
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val userRepository: UserRepository
) {

    private val logger = LoggerFactory.getLogger(CreditCardCostService::class.java)

    /**
     * Calculate the billing cycle window for a given statement close day and month.
     * Cycle starts the day after close in the previous month, ends on close day of target month.
     */
    private fun calculateBillingCycleWindow(statementCloseDay: Int, yearMonth: YearMonth): Pair<LocalDate, LocalDate> {
        val previousMonth = yearMonth.minusMonths(1)
        val startDay = minOf(statementCloseDay, previousMonth.lengthOfMonth())
        val start = previousMonth.atDay(startDay).plusDays(1)
        val endDay = minOf(statementCloseDay, yearMonth.lengthOfMonth())
        val end = yearMonth.atDay(endDay)
        return Pair(start, end)
    }

    /**
     * Get monthly cost for a single card in a specific billing cycle.
     */
    fun getCardMonthlyCost(cardId: UUID, yearMonth: YearMonth, userId: UUID): CardMonthlyCostResponse {
        logger.debug("Getting card monthly cost for card: $cardId, month: $yearMonth")
        val card = creditCardRepository.findById(cardId)
            ?: throw EntityNotFoundException("CreditCard", cardId)
        validateBudgetAccess(card.budget.id, userId)

        val (start, end) = calculateBillingCycleWindow(card.statementCloseDay, yearMonth)
        val transactions = transactionRepository.findByCardAndDateBetween(card, start, end)
        val totalCost = transactions.sumOf { it.amount }

        return CardMonthlyCostResponse(
            cardId = card.id.toString(),
            cardName = card.name,
            billingCycleStart = start,
            billingCycleEnd = end,
            totalCost = totalCost,
            transactionCount = transactions.size
        )
    }

    /**
     * Get cost summary for all active cards in a budget for a given month.
     */
    fun getAllCardsCostSummary(budgetId: UUID, yearMonth: YearMonth, userId: UUID): AllCardsCostSummaryResponse {
        logger.debug("Getting all cards cost summary for budget: $budgetId, month: $yearMonth")
        validateBudgetAccess(budgetId, userId)

        val budget = budgetRepository.findById(budgetId)
            ?: throw EntityNotFoundException("Budget", budgetId)
        val activeCards = creditCardRepository.findByBudgetAndActive(budget, true)

        val cardCosts = activeCards.map { card ->
            val (start, end) = calculateBillingCycleWindow(card.statementCloseDay, yearMonth)
            val transactions = transactionRepository.findByCardAndDateBetween(card, start, end)
            val totalCost = transactions.sumOf { it.amount }

            CardMonthlyCostResponse(
                cardId = card.id.toString(),
                cardName = card.name,
                billingCycleStart = start,
                billingCycleEnd = end,
                totalCost = totalCost,
                transactionCount = transactions.size
            )
        }.sortedByDescending { it.totalCost }

        val grandTotal = cardCosts.sumOf { it.totalCost }

        return AllCardsCostSummaryResponse(
            budgetId = budgetId.toString(),
            period = yearMonth.toString(),
            cards = cardCosts,
            grandTotal = grandTotal
        )
    }

    /**
     * Get cost comparison for a single card over the last N months.
     */
    fun getCardCostComparison(cardId: UUID, monthsCount: Int, userId: UUID): CardCostComparisonResponse {
        logger.debug("Getting card cost comparison for card: $cardId, months: $monthsCount")
        val card = creditCardRepository.findById(cardId)
            ?: throw EntityNotFoundException("CreditCard", cardId)
        validateBudgetAccess(card.budget.id, userId)

        val currentMonth = YearMonth.now()
        val months = (monthsCount - 1 downTo 0).map { i ->
            val yearMonth = currentMonth.minusMonths(i.toLong())
            val (start, end) = calculateBillingCycleWindow(card.statementCloseDay, yearMonth)
            val transactions = transactionRepository.findByCardAndDateBetween(card, start, end)
            val totalCost = transactions.sumOf { it.amount }

            CardMonthlyCostResponse(
                cardId = card.id.toString(),
                cardName = card.name,
                billingCycleStart = start,
                billingCycleEnd = end,
                totalCost = totalCost,
                transactionCount = transactions.size
            )
        }

        return CardCostComparisonResponse(
            cardId = card.id.toString(),
            cardName = card.name,
            months = months
        )
    }

    private fun validateBudgetAccess(budgetId: UUID, userId: UUID) {
        val budget = budgetRepository.findById(budgetId)
            ?: throw EntityNotFoundException("Budget", budgetId)
        val user = userRepository.findById(userId)
            ?: throw EntityNotFoundException("User", userId)
        if (!budget.hasMember(user) && !budget.isOwner(user)) {
            throw AccessDeniedException("User does not have access to this budget", "Budget", budgetId)
        }
    }
}
