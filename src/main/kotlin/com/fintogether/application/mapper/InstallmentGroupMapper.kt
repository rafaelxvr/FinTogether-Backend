package com.fintogether.application.mapper

import com.fintogether.application.dto.InstallmentGroupProgressResponse
import com.fintogether.application.dto.InstallmentGroupResponse
import com.fintogether.domain.entity.InstallmentGroup
import com.fintogether.domain.entity.Transaction
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate

@Component
class InstallmentGroupMapper(
    private val transactionMapper: TransactionMapper
) {

    fun toResponse(entity: InstallmentGroup): InstallmentGroupResponse {
        return InstallmentGroupResponse(
            id = entity.id,
            budgetId = entity.budget.id,
            cardId = entity.card?.id,
            totalAmount = entity.totalAmount,
            installmentCount = entity.installmentCount,
            interestRate = entity.interestRate,
            startDate = entity.startDate,
            description = entity.description,
            createdAt = entity.createdAt,
            installmentAmount = entity.calculateInstallmentAmount(),
            totalWithInterest = entity.calculateTotalWithInterest(),
            status = entity.status.name
        )
    }

    fun toProgressResponse(
        entity: InstallmentGroup,
        transactions: List<Transaction>
    ): InstallmentGroupProgressResponse {
        val today = LocalDate.now()
        val paidTransactions = transactions.filter { it.transactionDate <= today }
        val paidCount = paidTransactions.size
        val paidAmount = paidTransactions.fold(BigDecimal.ZERO) { acc, t -> acc.add(t.amount) }
        val remainingCount = entity.installmentCount - paidCount
        val totalWithInterest = entity.calculateTotalWithInterest()
        val remainingAmount = totalWithInterest.subtract(paidAmount)

        return InstallmentGroupProgressResponse(
            id = entity.id,
            budgetId = entity.budget.id,
            cardId = entity.card?.id,
            totalAmount = entity.totalAmount,
            installmentCount = entity.installmentCount,
            interestRate = entity.interestRate,
            startDate = entity.startDate,
            description = entity.description,
            createdAt = entity.createdAt,
            installmentAmount = entity.calculateInstallmentAmount(),
            totalWithInterest = totalWithInterest,
            status = entity.status.name,
            paidCount = paidCount,
            paidAmount = paidAmount,
            remainingCount = remainingCount,
            remainingAmount = remainingAmount,
            transactions = transactions.map { transactionMapper.toResponse(it) }
        )
    }
}
