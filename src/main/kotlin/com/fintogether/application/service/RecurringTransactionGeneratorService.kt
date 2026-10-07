package com.fintogether.application.service

import com.fintogether.domain.entity.RecurringTransactionTemplate
import com.fintogether.domain.entity.Transaction
import com.fintogether.domain.entity.TransactionType
import com.fintogether.domain.entity.TransactionVisibility
import com.fintogether.domain.repository.RecurringTransactionTemplateRepository
import com.fintogether.domain.repository.TransactionRepository
import com.fintogether.domain.valueobject.RecurrenceRule
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class RecurringTransactionGeneratorService(
    private val templateRepository: RecurringTransactionTemplateRepository,
    private val transactionRepository: TransactionRepository
) {

    private val logger = LoggerFactory.getLogger(RecurringTransactionGeneratorService::class.java)

    /**
     * Generates transactions for all active templates whose nextDueDate is on or before the given date.
     * For each template, creates a Transaction, updates the template's lastGeneratedDate and nextDueDate,
     * and deactivates the template if the recurrence has expired.
     *
     * @return the number of transactions generated
     */
    @Transactional
    fun generateDueTransactions(asOfDate: LocalDate): Int {
        logger.info("Starting recurring transaction generation for date: $asOfDate")

        val dueTemplates = templateRepository.findByIsActiveAndNextDueDateLessThanEqual(true, asOfDate)
        logger.info("Found ${dueTemplates.size} due templates")

        var generatedCount = 0

        for (template in dueTemplates) {
            try {
                generateTransactionFromTemplate(template, asOfDate)
                generatedCount++
            } catch (e: Exception) {
                logger.error("Failed to generate transaction from template ${template.id}: ${e.message}", e)
            }
        }

        logger.info("Recurring transaction generation complete. Generated $generatedCount transactions")
        return generatedCount
    }

    private fun generateTransactionFromTemplate(template: RecurringTransactionTemplate, asOfDate: LocalDate) {
        val transactionType = try {
            TransactionType.valueOf(template.type.uppercase())
        } catch (e: IllegalArgumentException) {
            logger.warn("Invalid transaction type '${template.type}' in template ${template.id}, defaulting to EXPENSE")
            TransactionType.EXPENSE
        }

        val visibility = try {
            TransactionVisibility.valueOf(template.visibility.uppercase())
        } catch (e: IllegalArgumentException) {
            TransactionVisibility.SHARED
        }

        val transaction = Transaction(
            budget = template.budget,
            user = template.user,
            transactionDate = template.nextDueDate,
            amount = template.amount,
            type = transactionType,
            account = template.account,
            category = template.category,
            card = template.card,
            description = template.description,
            notes = template.notes,
            visibility = visibility,
            isRecurring = true,
            recurrenceRule = template.recurrenceRule,
            parentRecurringId = template.id
        )

        transactionRepository.save(transaction)
        logger.debug("Created transaction for template ${template.id} on date ${template.nextDueDate}")

        // Update the template
        template.markGenerated(template.nextDueDate)

        // Compute next occurrence
        val recurrenceRule = RecurrenceRule.fromMap(template.recurrenceRule)
        val nextDate = recurrenceRule.computeNextOccurrence(template.nextDueDate)

        val generatedCount = transactionRepository.countByParentRecurringId(template.id)

        if (nextDate == null || recurrenceRule.isExpired(asOfDate, generatedCount.toInt())) {
            logger.info("Template ${template.id} has expired, deactivating")
            template.deactivate()
        } else {
            template.updateNextDueDate(nextDate)
        }

        templateRepository.save(template)
    }
}
