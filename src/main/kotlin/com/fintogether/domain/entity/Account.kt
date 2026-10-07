package com.fintogether.domain.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "accounts")
class Account(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "budget_id", nullable = false)
    val budget: Budget,

    @Column(name = "name", nullable = false)
    var name: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    var type: AccountType,

    @Column(name = "currency", nullable = false)
    var currency: String = "BRL",

    @Column(name = "opening_balance", precision = 15, scale = 2, nullable = false)
    val openingBalance: BigDecimal = BigDecimal.ZERO,

    @Column(name = "current_balance", precision = 15, scale = 2, nullable = false)
    var currentBalance: BigDecimal = BigDecimal.ZERO,

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

    fun update(name: String? = null, currency: String? = null) {
        name?.let { this.name = it }
        currency?.let { this.currency = it }
    }

    fun deactivate() {
        this.active = false
    }

    fun activate() {
        this.active = true
    }

    fun addAmount(amount: BigDecimal) {
        if (amount < BigDecimal.ZERO) {
            throw IllegalArgumentException("Amount to add must be positive")
        }
        this.currentBalance = this.currentBalance.add(amount)
    }

    fun subtractAmount(amount: BigDecimal) {
        if (amount < BigDecimal.ZERO) {
            throw IllegalArgumentException("Amount to subtract must be positive")
        }
        this.currentBalance = this.currentBalance.subtract(amount)
    }

    fun updateBalance(newBalance: BigDecimal) {
        this.currentBalance = newBalance
    }

    fun hasPositiveBalance(): Boolean = currentBalance > BigDecimal.ZERO

    fun isActive(): Boolean = active

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Account) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String {
        return "Account(id=$id, name='$name', type=$type, balance=$currentBalance)"
    }
}

enum class AccountType {
    CASH,
    CHECKING,
    SAVINGS,
    CREDIT_CARD,
    INVESTMENT
}
