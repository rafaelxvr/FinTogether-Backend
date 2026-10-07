package com.fintogether.domain.repository

import com.fintogether.domain.entity.TransactionSplit
import java.util.UUID

interface TransactionSplitRepository {
    fun save(split: TransactionSplit): TransactionSplit
    fun saveAll(splits: List<TransactionSplit>): List<TransactionSplit>
    fun findById(id: UUID): TransactionSplit?
    fun findByTransactionId(transactionId: UUID): List<TransactionSplit>
    fun findByUserIdAndIsPaidFalse(userId: UUID): List<TransactionSplit>
    fun findByBudgetId(budgetId: UUID): List<TransactionSplit>
    fun delete(split: TransactionSplit)
}
