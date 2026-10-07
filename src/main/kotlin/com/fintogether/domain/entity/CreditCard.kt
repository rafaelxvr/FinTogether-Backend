package com.fintogether.domain.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "credit_cards")
class CreditCard(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "budget_id", nullable = false)
    val budget: Budget,

    @Column(name = "name", nullable = false)
    var name: String,

    @Column(name = "issuer")
    var issuer: String? = null,

    @Column(name = "last_four")
    var lastFour: String? = null,

    @Column(name = "currency", nullable = false)
    var currency: String = "BRL",

    @Column(name = "statement_close_day", nullable = false)
    var statementCloseDay: Int,

    @Column(name = "due_day", nullable = false)
    var dueDay: Int,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_account_id")
    var linkedAccount: Account? = null,

    @Column(name = "credit_limit", precision = 15, scale = 2)
    var creditLimit: BigDecimal? = null,

    @Column(name = "active", nullable = false)
    var active: Boolean = true,

    id: UUID = UUID.randomUUID(),
    createdAt: LocalDateTime = LocalDateTime.now()
) {
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID = id

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = createdAt

    init {
        validateCloseDayAndDueDay()
    }

    private fun validateCloseDayAndDueDay() {
        require(statementCloseDay in 1..31) {
            "Statement close day must be between 1 and 31"
        }
        require(dueDay in 1..31) {
            "Due day must be between 1 and 31"
        }
    }

    fun update(
        name: String? = null,
        issuer: String? = null,
        lastFour: String? = null,
        currency: String? = null,
        statementCloseDay: Int? = null,
        dueDay: Int? = null,
        linkedAccount: Account? = null,
        creditLimit: BigDecimal? = null
    ) {
        name?.let { this.name = it }
        issuer?.let { this.issuer = it }
        lastFour?.let { this.lastFour = it }
        currency?.let { this.currency = it }
        statementCloseDay?.let {
            this.statementCloseDay = it
            validateCloseDayAndDueDay()
        }
        dueDay?.let {
            this.dueDay = it
            validateCloseDayAndDueDay()
        }
        this.linkedAccount = linkedAccount
        this.creditLimit = creditLimit
    }

    /**
     * Calculate available credit based on the limit and current spending.
     * Returns null if no credit limit is set.
     */
    fun availableCredit(currentSpending: BigDecimal): BigDecimal? {
        return creditLimit?.subtract(currentSpending)
    }

    fun deactivate() {
        this.active = false
    }

    fun activate() {
        this.active = true
    }

    fun linkAccount(account: Account) {
        this.linkedAccount = account
    }

    fun unlinkAccount() {
        this.linkedAccount = null
    }

    fun isActive(): Boolean = active

    fun hasLinkedAccount(): Boolean = linkedAccount != null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CreditCard) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String {
        return "CreditCard(id=$id, name='$name', issuer=$issuer, lastFour=$lastFour)"
    }
}
