package com.fintogether.application.service

import com.fintogether.application.dto.*
import com.fintogether.domain.entity.CreditCard
import com.fintogether.domain.entity.CreditCardPayment
import com.fintogether.domain.entity.Transaction
import com.fintogether.domain.entity.TransactionType
import com.fintogether.domain.exception.AccessDeniedException
import com.fintogether.domain.exception.EntityNotFoundException
import com.fintogether.domain.repository.AccountRepository
import com.fintogether.domain.repository.BudgetRepository
import com.fintogether.domain.repository.CreditCardPaymentRepository
import com.fintogether.domain.repository.CreditCardRepository
import com.fintogether.domain.repository.TransactionRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

/**
 * Credit Card Invoice Service
 * Handles business logic for computing credit card invoices from transactions,
 * marking invoices as paid, and detecting payment suggestions.
 */
@Service
@Transactional(readOnly = true)
class CreditCardInvoiceService(
    private val creditCardRepository: CreditCardRepository,
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val creditCardPaymentRepository: CreditCardPaymentRepository,
    private val accountRepository: AccountRepository
) {

    private val logger = LoggerFactory.getLogger(CreditCardInvoiceService::class.java)

    /**
     * Get a full invoice for a specific card and reference month.
     * Computes period dates from statementCloseDay, queries transactions,
     * sums amounts, and determines invoice status.
     */
    fun getInvoice(cardId: UUID, yearMonth: YearMonth, userId: UUID): CreditCardInvoiceResponse {
        logger.debug("Getting invoice for card: $cardId, month: $yearMonth")

        val creditCard = findAndValidateCard(cardId, userId)

        val (periodStart, periodEnd) = computePeriodDates(creditCard.statementCloseDay, yearMonth)
        val dueDate = computeDueDate(creditCard.dueDay, periodEnd)
        val status = determineStatus(cardId, yearMonth, periodEnd, dueDate)

        val transactions = transactionRepository.findByCardAndDateBetween(creditCard, periodStart, periodEnd)
        val totalAmount = transactions.sumOf { it.amount }

        val transactionDtos = transactions
            .sortedByDescending { it.transactionDate }
            .map { toInvoiceTransactionDto(it) }

        return CreditCardInvoiceResponse(
            cardId = creditCard.id.toString(),
            cardName = creditCard.name,
            referenceMonth = yearMonth.toString(),
            periodStart = periodStart,
            periodEnd = periodEnd,
            dueDate = dueDate,
            totalAmount = totalAmount,
            transactionCount = transactions.size,
            transactions = transactionDtos,
            status = status,
            creditLimit = creditCard.creditLimit,
            availableCredit = creditCard.creditLimit?.let { it.subtract(totalAmount) }
        )
    }

    /**
     * Get lightweight invoice summaries for the last N months.
     */
    fun getInvoiceSummaries(cardId: UUID, monthsBack: Int, userId: UUID): InvoiceSummaryResponse {
        logger.debug("Getting invoice summaries for card: $cardId, monthsBack: $monthsBack")

        val creditCard = findAndValidateCard(cardId, userId)
        val currentMonth = YearMonth.now()

        val summaries = (0 until monthsBack).map { offset ->
            val yearMonth = currentMonth.minusMonths(offset.toLong())
            val (periodStart, periodEnd) = computePeriodDates(creditCard.statementCloseDay, yearMonth)
            val dueDate = computeDueDate(creditCard.dueDay, periodEnd)
            val status = determineStatus(cardId, yearMonth, periodEnd, dueDate)

            val transactions = transactionRepository.findByCardAndDateBetween(creditCard, periodStart, periodEnd)
            val totalAmount = transactions.sumOf { it.amount }

            InvoiceSummaryDto(
                referenceMonth = yearMonth.toString(),
                periodStart = periodStart,
                periodEnd = periodEnd,
                dueDate = dueDate,
                totalAmount = totalAmount,
                transactionCount = transactions.size,
                status = status
            )
        }

        return InvoiceSummaryResponse(
            cardId = creditCard.id.toString(),
            cardName = creditCard.name,
            invoices = summaries
        )
    }

    /**
     * Mark a credit card invoice as paid.
     */
    @Transactional
    fun markInvoicePaid(cardId: UUID, yearMonth: YearMonth, request: MarkInvoicePaidRequest, userId: UUID): CreditCardPaymentResponse {
        logger.debug("Marking invoice as paid for card: $cardId, month: $yearMonth")
        val creditCard = findAndValidateCard(cardId, userId)

        // Check not already paid
        val existing = creditCardPaymentRepository.findByCreditCardIdAndReferenceMonth(
            cardId, yearMonth.atDay(1)
        )
        if (existing != null) {
            throw IllegalStateException("Invoice for $yearMonth is already marked as paid")
        }

        // Validate payment account if provided
        val paymentAccount = request.paymentAccountId?.let { accountId ->
            accountRepository.findById(accountId)
                ?: throw EntityNotFoundException("Account", accountId)
        }

        val payment = CreditCardPayment(
            creditCard = creditCard,
            referenceMonth = yearMonth.atDay(1),
            paidAmount = request.paidAmount,
            paymentAccount = paymentAccount,
            notes = request.notes
        )

        val saved = creditCardPaymentRepository.save(payment)

        return CreditCardPaymentResponse(
            id = saved.id.toString(),
            creditCardId = cardId.toString(),
            referenceMonth = yearMonth.toString(),
            paidAmount = saved.paidAmount,
            paidAt = saved.paidAt,
            paymentAccountId = paymentAccount?.id?.toString(),
            paymentAccountName = paymentAccount?.name,
            notes = saved.notes,
            createdAt = saved.createdAt
        )
    }

    /**
     * Get payment record for an invoice.
     */
    fun getPayment(cardId: UUID, yearMonth: YearMonth, userId: UUID): CreditCardPaymentResponse? {
        findAndValidateCard(cardId, userId)

        val payment = creditCardPaymentRepository.findByCreditCardIdAndReferenceMonth(
            cardId, yearMonth.atDay(1)
        ) ?: return null

        return CreditCardPaymentResponse(
            id = payment.id.toString(),
            creditCardId = cardId.toString(),
            referenceMonth = yearMonth.toString(),
            paidAmount = payment.paidAmount,
            paidAt = payment.paidAt,
            paymentAccountId = payment.paymentAccount?.id?.toString(),
            paymentAccountName = payment.paymentAccount?.name,
            notes = payment.notes,
            createdAt = payment.createdAt
        )
    }

    /**
     * Detect a payment suggestion by matching transfers near the due date.
     * Looks for exact-amount INCOME transactions to the card's linked account
     * within 5 days of the due date.
     */
    fun detectPaymentSuggestion(cardId: UUID, yearMonth: YearMonth, userId: UUID): PaymentSuggestionResponse {
        val creditCard = findAndValidateCard(cardId, userId)

        // Check if already paid
        val existingPayment = creditCardPaymentRepository.findByCreditCardIdAndReferenceMonth(
            cardId, yearMonth.atDay(1)
        )
        if (existingPayment != null) {
            return PaymentSuggestionResponse(hasSuggestion = false)
        }

        val linkedAccount = creditCard.linkedAccount
            ?: return PaymentSuggestionResponse(hasSuggestion = false)

        val (_, periodEnd) = computePeriodDates(creditCard.statementCloseDay, yearMonth)
        val dueDate = computeDueDate(creditCard.dueDay, periodEnd)

        // Get invoice total
        val invoice = getInvoice(cardId, yearMonth, userId)
        if (invoice.totalAmount <= BigDecimal.ZERO) {
            return PaymentSuggestionResponse(hasSuggestion = false)
        }

        // Look for transfers to the card's linked account within 5 days of due date
        val searchStart = dueDate.minusDays(5)
        val searchEnd = dueDate.plusDays(5)

        // Search transactions on the linked account's budget within the date range
        val candidateTransactions = transactionRepository.findByBudgetIdAndDateBetween(
            linkedAccount.budget.id, searchStart, searchEnd
        ).filter {
            it.account?.id == linkedAccount.id &&
            it.type == TransactionType.INCOME &&
            it.amount.compareTo(invoice.totalAmount) == 0
        }

        if (candidateTransactions.isNotEmpty()) {
            val match = candidateTransactions.first()
            return PaymentSuggestionResponse(
                hasSuggestion = true,
                suggestedTransactionId = match.id.toString(),
                suggestedAmount = match.amount,
                suggestedAccountName = linkedAccount.name,
                suggestedDate = match.transactionDate,
                message = "A transfer of ${match.amount} was found on ${match.transactionDate} -- confirm this as payment?"
            )
        }

        return PaymentSuggestionResponse(hasSuggestion = false)
    }

    /**
     * Compute the billing period dates based on the statement close day and reference month.
     */
    fun computePeriodDates(statementCloseDay: Int, referenceMonth: YearMonth): Pair<LocalDate, LocalDate> {
        val previousMonth = referenceMonth.minusMonths(1)

        val periodStartDay = minOf(statementCloseDay, previousMonth.lengthOfMonth())
        val periodStart = previousMonth.atDay(periodStartDay).plusDays(1)

        val periodEndDay = minOf(statementCloseDay, referenceMonth.lengthOfMonth())
        val periodEnd = referenceMonth.atDay(periodEndDay)

        return Pair(periodStart, periodEnd)
    }

    /**
     * Compute the due date based on the due day.
     */
    fun computeDueDate(dueDay: Int, closeDate: LocalDate): LocalDate {
        val dueMonth = YearMonth.from(closeDate).plusMonths(1)
        val actualDueDay = minOf(dueDay, dueMonth.lengthOfMonth())
        return dueMonth.atDay(actualDueDay)
    }

    /**
     * Determine the invoice status. Checks for payment record first,
     * then falls back to date-based status determination.
     */
    fun determineStatus(cardId: UUID, referenceMonth: YearMonth, closeDate: LocalDate, dueDate: LocalDate): InvoiceStatus {
        val payment = creditCardPaymentRepository.findByCreditCardIdAndReferenceMonth(
            cardId, referenceMonth.atDay(1)
        )
        if (payment != null) {
            return InvoiceStatus.PAID
        }

        val today = LocalDate.now()
        return when {
            today.isAfter(dueDate) -> InvoiceStatus.OVERDUE
            today.isAfter(closeDate) -> InvoiceStatus.CLOSED
            else -> InvoiceStatus.OPEN
        }
    }

    /**
     * Legacy overload for backward compatibility.
     */
    fun determineStatus(closeDate: LocalDate, dueDate: LocalDate): InvoiceStatus {
        val today = LocalDate.now()
        return when {
            today.isAfter(dueDate) -> InvoiceStatus.OVERDUE
            today.isAfter(closeDate) -> InvoiceStatus.CLOSED
            else -> InvoiceStatus.OPEN
        }
    }

    /**
     * Find a credit card and validate that the user has access to its budget.
     */
    private fun findAndValidateCard(cardId: UUID, userId: UUID): CreditCard {
        val creditCard = creditCardRepository.findById(cardId)
            ?: throw EntityNotFoundException("CreditCard", cardId)

        validateBudgetMembership(creditCard.budget, userId)
        return creditCard
    }

    /**
     * Validate that user is a member of the budget.
     */
    private fun validateBudgetMembership(budget: com.fintogether.domain.entity.Budget, userId: UUID) {
        val isMember = budget.members.any { it.user.id == userId }
        if (!isMember) {
            logger.warn("Access denied for user $userId to budget ${budget.id}")
            throw AccessDeniedException("User is not a member of this budget", "Budget", budget.id)
        }
    }

    /**
     * Convert a Transaction entity to an InvoiceTransactionDto.
     */
    private fun toInvoiceTransactionDto(transaction: Transaction): InvoiceTransactionDto {
        val installmentInfo = if (transaction.isInstallment && transaction.installmentNumber != null) {
            "Installment ${transaction.installmentNumber}"
        } else {
            null
        }

        return InvoiceTransactionDto(
            id = transaction.id.toString(),
            transactionDate = transaction.transactionDate,
            description = transaction.description,
            categoryName = transaction.category?.name,
            categoryId = transaction.category?.id?.toString(),
            amount = transaction.amount,
            installmentInfo = installmentInfo
        )
    }
}
