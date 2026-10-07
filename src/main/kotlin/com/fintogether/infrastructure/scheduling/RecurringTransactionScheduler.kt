package com.fintogether.infrastructure.scheduling

import com.fintogether.application.service.RecurringTransactionGeneratorService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class RecurringTransactionScheduler(
    private val generatorService: RecurringTransactionGeneratorService
) {

    private val logger = LoggerFactory.getLogger(RecurringTransactionScheduler::class.java)

    /**
     * Runs daily at 2:00 AM to generate recurring transactions that are due.
     */
    @Scheduled(cron = "0 0 2 * * *")
    fun generateRecurringTransactions() {
        val today = LocalDate.now()
        logger.info("Scheduled recurring transaction generation started for date: $today")

        try {
            val count = generatorService.generateDueTransactions(today)
            logger.info("Scheduled recurring transaction generation completed. Generated $count transactions")
        } catch (e: Exception) {
            logger.error("Scheduled recurring transaction generation failed: ${e.message}", e)
        }
    }
}
