package com.fintogether.infrastructure.persistence

import com.fintogether.domain.entity.TransactionSplit
import com.fintogether.domain.repository.TransactionSplitRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

interface JpaTransactionSplitRepositoryInterface : JpaRepository<TransactionSplit, UUID> {
    fun findByTransactionId(transactionId: UUID): List<TransactionSplit>
    fun findByUserIdAndIsPaidFalse(userId: UUID): List<TransactionSplit>

    @Query("SELECT s FROM TransactionSplit s WHERE s.transaction.budget.id = :budgetId")
    fun findByBudgetId(budgetId: UUID): List<TransactionSplit>
}

@Repository
class JpaTransactionSplitRepository(
    private val jpaRepository: JpaTransactionSplitRepositoryInterface
) : TransactionSplitRepository {

    override fun save(split: TransactionSplit): TransactionSplit = jpaRepository.save(split)

    override fun saveAll(splits: List<TransactionSplit>): List<TransactionSplit> = jpaRepository.saveAll(splits)

    override fun findById(id: UUID): TransactionSplit? = jpaRepository.findById(id).orElse(null)

    override fun findByTransactionId(transactionId: UUID): List<TransactionSplit> =
        jpaRepository.findByTransactionId(transactionId)

    override fun findByUserIdAndIsPaidFalse(userId: UUID): List<TransactionSplit> =
        jpaRepository.findByUserIdAndIsPaidFalse(userId)

    override fun findByBudgetId(budgetId: UUID): List<TransactionSplit> =
        jpaRepository.findByBudgetId(budgetId)

    override fun delete(split: TransactionSplit) = jpaRepository.delete(split)
}
