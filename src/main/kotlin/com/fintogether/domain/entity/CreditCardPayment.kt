package com.fintogether.domain.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "credit_card_payments",
    uniqueConstraints = [UniqueConstraint(columnNames = ["credit_card_id", "reference_month"])]
)
class CreditCardPayment(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_card_id", nullable = false)
    val creditCard: CreditCard,

    @Column(name = "reference_month", nullable = false)
    val referenceMonth: LocalDate,

    @Column(name = "paid_amount", nullable = false, precision = 15, scale = 2)
    val paidAmount: BigDecimal,

    @Column(name = "paid_at", nullable = false)
    val paidAt: LocalDateTime = LocalDateTime.now(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_account_id")
    val paymentAccount: Account? = null,

    @Column(name = "notes", length = 500)
    var notes: String? = null,

    id: UUID = UUID.randomUUID(),
    createdAt: LocalDateTime = LocalDateTime.now()
) {
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID = id

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = createdAt

    init {
        require(paidAmount > BigDecimal.ZERO) { "Paid amount must be positive" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CreditCardPayment) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String {
        return "CreditCardPayment(id=$id, cardId=${creditCard.id}, referenceMonth=$referenceMonth, paidAmount=$paidAmount)"
    }
}
