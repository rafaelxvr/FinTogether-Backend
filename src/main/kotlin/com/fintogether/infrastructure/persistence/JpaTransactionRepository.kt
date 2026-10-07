package com.fintogether.infrastructure.persistence

import com.fintogether.domain.entity.Budget
import com.fintogether.domain.entity.Category
import com.fintogether.domain.entity.CreditCard
import com.fintogether.domain.entity.Transaction
import com.fintogether.domain.entity.TransactionType
import com.fintogether.domain.repository.TransactionRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

interface JpaTransactionRepositoryInterface : JpaRepository<Transaction, UUID> {
    fun findByBudget(budget: Budget): List<Transaction>
    fun findByBudgetId(budgetId: UUID): List<Transaction>
    fun findByBudgetId(budgetId: UUID, pageable: Pageable): Page<Transaction>
    fun findByBudgetAndTransactionDateBetween(budget: Budget, startDate: LocalDate, endDate: LocalDate): List<Transaction>
    fun findByBudgetIdAndTransactionDateBetween(budgetId: UUID, startDate: LocalDate, endDate: LocalDate): List<Transaction>
    fun findByBudgetIdAndTransactionDateBetween(budgetId: UUID, startDate: LocalDate, endDate: LocalDate, pageable: Pageable): Page<Transaction>
    fun findByCategory(category: Category): List<Transaction>
    fun findByCard(card: CreditCard): List<Transaction>
    fun findByCardAndTransactionDateBetween(card: CreditCard, startDate: LocalDate, endDate: LocalDate): List<Transaction>
    fun findByInstallmentGroupId(groupId: UUID): List<Transaction>
    fun findByParentRecurringId(parentRecurringId: UUID): List<Transaction>
    fun countByParentRecurringId(parentRecurringId: UUID): Long
    @Query("""
        SELECT t FROM Transaction t
        WHERE t.budget.id = :budgetId
        AND (:query IS NULL OR LOWER(t.description) LIKE LOWER(CONCAT('%', :query, '%'))
             OR LOWER(t.notes) LIKE LOWER(CONCAT('%', :query, '%')))
        AND (:type IS NULL OR t.type = :type)
        AND (:categoryId IS NULL OR t.category.id = :categoryId)
        AND (:merchantId IS NULL OR t.merchant.id = :merchantId)
        AND (:minAmount IS NULL OR t.amount >= :minAmount)
        AND (:maxAmount IS NULL OR t.amount <= :maxAmount)
        AND (:startDate IS NULL OR t.transactionDate >= :startDate)
        AND (:endDate IS NULL OR t.transactionDate <= :endDate)
        ORDER BY t.transactionDate DESC
    """)
    fun searchTransactions(
        @Param("budgetId") budgetId: UUID,
        @Param("query") query: String?,
        @Param("type") type: TransactionType?,
        @Param("categoryId") categoryId: UUID?,
        @Param("merchantId") merchantId: UUID?,
        @Param("minAmount") minAmount: BigDecimal?,
        @Param("maxAmount") maxAmount: BigDecimal?,
        @Param("startDate") startDate: LocalDate?,
        @Param("endDate") endDate: LocalDate?,
        pageable: Pageable
    ): Page<Transaction>
}

@Repository
class JpaTransactionRepository(
    private val jpaRepository: JpaTransactionRepositoryInterface
) : TransactionRepository {

    override fun save(transaction: Transaction): Transaction = jpaRepository.save(transaction)

    override fun saveAll(transactions: List<Transaction>): List<Transaction> = jpaRepository.saveAll(transactions)

    override fun findById(id: UUID): Transaction? = jpaRepository.findById(id).orElse(null)

    override fun findByBudget(budget: Budget): List<Transaction> = jpaRepository.findByBudget(budget)

    override fun findByBudgetId(budgetId: UUID): List<Transaction> = jpaRepository.findByBudgetId(budgetId)

    override fun findByBudgetId(budgetId: UUID, pageable: Pageable): Page<Transaction> =
        jpaRepository.findByBudgetId(budgetId, pageable)

    override fun findByBudgetAndDateBetween(budget: Budget, startDate: LocalDate, endDate: LocalDate): List<Transaction> =
        jpaRepository.findByBudgetAndTransactionDateBetween(budget, startDate, endDate)

    override fun findByBudgetIdAndDateBetween(budgetId: UUID, startDate: LocalDate, endDate: LocalDate): List<Transaction> =
        jpaRepository.findByBudgetIdAndTransactionDateBetween(budgetId, startDate, endDate)

    override fun findByBudgetIdAndDateBetween(budgetId: UUID, startDate: LocalDate, endDate: LocalDate, pageable: Pageable): Page<Transaction> =
        jpaRepository.findByBudgetIdAndTransactionDateBetween(budgetId, startDate, endDate, pageable)

    override fun findByCategory(category: Category): List<Transaction> = jpaRepository.findByCategory(category)

    override fun findByCard(card: CreditCard): List<Transaction> = jpaRepository.findByCard(card)

    override fun findByCardAndDateBetween(card: CreditCard, startDate: LocalDate, endDate: LocalDate): List<Transaction> =
        jpaRepository.findByCardAndTransactionDateBetween(card, startDate, endDate)

    override fun findByInstallmentGroupId(groupId: UUID): List<Transaction> =
        jpaRepository.findByInstallmentGroupId(groupId)

    override fun findByParentRecurringId(parentRecurringId: UUID): List<Transaction> =
        jpaRepository.findByParentRecurringId(parentRecurringId)

    override fun countByParentRecurringId(parentRecurringId: UUID): Long =
        jpaRepository.countByParentRecurringId(parentRecurringId)

    override fun search(
        budgetId: UUID,
        query: String?,
        type: TransactionType?,
        categoryId: UUID?,
        merchantId: UUID?,
        minAmount: BigDecimal?,
        maxAmount: BigDecimal?,
        startDate: LocalDate?,
        endDate: LocalDate?,
        pageable: Pageable
    ): Page<Transaction> = jpaRepository.searchTransactions(
        budgetId, query, type, categoryId, merchantId,
        minAmount, maxAmount, startDate, endDate, pageable
    )

    override fun delete(transaction: Transaction) = jpaRepository.delete(transaction)

    override fun deleteAll(transactions: List<Transaction>) = jpaRepository.deleteAll(transactions)
}
