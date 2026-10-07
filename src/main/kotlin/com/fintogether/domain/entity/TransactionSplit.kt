package com.fintogether.domain.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "transaction_splits",
    uniqueConstraints = [UniqueConstraint(name = "uk_split_transaction_user", columnNames = ["transaction_id", "user_id"])]
)
class TransactionSplit(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    val transaction: Transaction,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(name = "amount", precision = 15, scale = 2, nullable = false)
    val amount: BigDecimal,

    @Column(name = "is_paid", nullable = false)
    var isPaid: Boolean = false,

    @Column(name = "paid_at")
    var paidAt: LocalDateTime? = null,

    id: UUID = UUID.randomUUID(),
    createdAt: LocalDateTime = LocalDateTime.now(),
    updatedAt: LocalDateTime = LocalDateTime.now()
) : BaseEntity(id, createdAt, updatedAt) {

    fun settle() {
        isPaid = true
        paidAt = LocalDateTime.now()
    }

    fun unsettle() {
        isPaid = false
        paidAt = null
    }

    fun isSettled(): Boolean = isPaid

    override fun toString(): String {
        return "TransactionSplit(id=$id, transactionId=${transaction.id}, userId=${user.id}, amount=$amount, isPaid=$isPaid)"
    }
}
