package com.fintogether.domain.entity

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "transactions")
class Transaction(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "budget_id", nullable = false)
    val budget: Budget,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: User? = null,

    @Column(name = "transaction_date", nullable = false)
    var transactionDate: LocalDate,

    @Column(name = "amount", precision = 15, scale = 2, nullable = false)
    var amount: BigDecimal,

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    var type: TransactionType,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    var account: Account? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    var category: Category? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id")
    var card: CreditCard? = null,

    @Column(name = "description")
    var description: String? = null,

    @Column(name = "notes")
    var notes: String? = null,

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "tags", columnDefinition = "text[]")
    var tags: List<String>? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false)
    var visibility: TransactionVisibility = TransactionVisibility.SHARED,

    @Column(name = "is_recurring", nullable = false)
    var isRecurring: Boolean = false,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "recurrence_rule", columnDefinition = "jsonb")
    var recurrenceRule: Map<String, Any>? = null,

    @Column(name = "is_installment", nullable = false)
    var isInstallment: Boolean = false,

    @Column(name = "installment_group_id")
    var installmentGroupId: UUID? = null,

    @Column(name = "installment_number")
    var installmentNumber: Int? = null,

    @Column(name = "parent_recurring_id")
    var parentRecurringId: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id")
    var merchant: Merchant? = null,

    id: UUID = UUID.randomUUID(),
    createdAt: LocalDateTime = LocalDateTime.now(),
    updatedAt: LocalDateTime = LocalDateTime.now()
) : BaseEntity(id, createdAt, updatedAt) {

    @Column(name = "synced_at")
    var syncedAt: LocalDateTime? = null

    fun update(
        transactionDate: LocalDate? = null,
        amount: BigDecimal? = null,
        description: String? = null,
        notes: String? = null,
        tags: List<String>? = null,
        category: Category? = null,
        merchant: Merchant? = null
    ) {
        transactionDate?.let { this.transactionDate = it }
        amount?.let { this.amount = it }
        description?.let { this.description = it }
        notes?.let { this.notes = it }
        tags?.let { this.tags = it }
        category?.let { this.category = it }
        this.merchant = merchant
    }

    fun updateVisibility(visibility: TransactionVisibility) {
        this.visibility = visibility
    }

    fun markAsSynced() {
        this.syncedAt = LocalDateTime.now()
    }

    fun isIncome(): Boolean = type == TransactionType.INCOME

    fun isExpense(): Boolean = type == TransactionType.EXPENSE

    fun isTransfer(): Boolean = type == TransactionType.TRANSFER

    fun isSynced(): Boolean = syncedAt != null

    fun isPrivate(): Boolean = visibility == TransactionVisibility.PRIVATE

    fun isShared(): Boolean = visibility == TransactionVisibility.SHARED

    fun belongsToCard(): Boolean = card != null

    override fun toString(): String {
        return "Transaction(id=$id, date=$transactionDate, amount=$amount, type=$type, description=$description)"
    }
}

enum class TransactionType {
    INCOME,
    EXPENSE,
    TRANSFER
}

enum class TransactionVisibility {
    PRIVATE,
    SHARED
}
