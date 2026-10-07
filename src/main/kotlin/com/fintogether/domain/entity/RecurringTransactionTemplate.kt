package com.fintogether.domain.entity

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "recurring_transaction_templates")
class RecurringTransactionTemplate(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "budget_id", nullable = false)
    val budget: Budget,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(name = "amount", precision = 15, scale = 2, nullable = false)
    var amount: BigDecimal,

    @Column(name = "type", nullable = false, length = 20)
    var type: String,

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

    @Column(name = "tags")
    var tags: String? = null,

    @Column(name = "visibility", nullable = false, length = 20)
    var visibility: String = "SHARED",

    @Column(name = "is_fixed_expense", nullable = false)
    var isFixedExpense: Boolean = false,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "recurrence_rule", columnDefinition = "jsonb", nullable = false)
    var recurrenceRule: Map<String, Any>,

    @Column(name = "last_generated_date")
    var lastGeneratedDate: LocalDate? = null,

    @Column(name = "next_due_date", nullable = false)
    var nextDueDate: LocalDate,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    id: UUID = UUID.randomUUID(),
    createdAt: LocalDateTime = LocalDateTime.now(),
    updatedAt: LocalDateTime = LocalDateTime.now()
) : BaseEntity(id, createdAt, updatedAt) {

    fun deactivate() {
        this.isActive = false
    }

    fun markGenerated(date: LocalDate) {
        this.lastGeneratedDate = date
    }

    fun updateNextDueDate(date: LocalDate) {
        this.nextDueDate = date
    }

    override fun toString(): String {
        return "RecurringTransactionTemplate(id=$id, type=$type, amount=$amount, nextDueDate=$nextDueDate, isActive=$isActive)"
    }
}
