package com.fintogether.domain.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

@Entity
@Table(name = "savings_goals")
class SavingsGoal(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "budget_id", nullable = false)
    val budget: Budget,

    @Column(name = "name", nullable = false)
    var name: String,

    @Column(name = "target_amount", precision = 15, scale = 2, nullable = false)
    var targetAmount: BigDecimal,

    @Column(name = "current_amount", precision = 15, scale = 2, nullable = false)
    var currentAmount: BigDecimal = BigDecimal.ZERO,

    @Column(name = "currency", nullable = false)
    var currency: String = "BRL",

    @Column(name = "description", length = 500)
    var description: String? = null,

    @Column(name = "deadline")
    var deadline: LocalDate? = null,

    @Column(name = "icon", length = 50)
    var icon: String? = null,

    @Column(name = "color", length = 7)
    var color: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    var account: Account? = null,

    @Column(name = "active", nullable = false)
    var active: Boolean = true,

    @Column(name = "completed_at")
    var completedAt: LocalDateTime? = null,

    id: UUID = UUID.randomUUID(),
    createdAt: LocalDateTime = LocalDateTime.now()
) {
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID = id

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = createdAt

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null

    fun contribute(amount: BigDecimal) {
        if (amount <= BigDecimal.ZERO) {
            throw IllegalArgumentException("Contribution amount must be positive")
        }
        this.currentAmount = this.currentAmount.add(amount)
        this.updatedAt = LocalDateTime.now()
        if (isCompleted() && completedAt == null) {
            this.completedAt = LocalDateTime.now()
        }
    }

    fun withdraw(amount: BigDecimal) {
        if (amount <= BigDecimal.ZERO) {
            throw IllegalArgumentException("Withdrawal amount must be positive")
        }
        if (this.currentAmount < amount) {
            throw IllegalArgumentException("Insufficient balance in savings goal")
        }
        this.currentAmount = this.currentAmount.subtract(amount)
        this.updatedAt = LocalDateTime.now()
        if (!isCompleted() && completedAt != null) {
            this.completedAt = null
        }
    }

    fun update(
        name: String? = null,
        description: String? = null,
        targetAmount: BigDecimal? = null,
        deadline: LocalDate? = null,
        icon: String? = null,
        color: String? = null
    ) {
        name?.let { this.name = it }
        description?.let { this.description = it }
        targetAmount?.let { this.targetAmount = it }
        deadline?.let { this.deadline = it }
        icon?.let { this.icon = it }
        color?.let { this.color = it }
        this.updatedAt = LocalDateTime.now()
    }

    fun deactivate() {
        this.active = false
        this.updatedAt = LocalDateTime.now()
    }

    fun activate() {
        this.active = true
        this.updatedAt = LocalDateTime.now()
    }

    fun progressPercentage(): Double {
        if (targetAmount <= BigDecimal.ZERO) return 0.0
        return currentAmount.divide(targetAmount, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal(100))
            .toDouble()
            .coerceAtMost(100.0)
    }

    fun isCompleted(): Boolean = currentAmount >= targetAmount

    fun remainingAmount(): BigDecimal =
        (targetAmount - currentAmount).coerceAtLeast(BigDecimal.ZERO)

    fun daysRemaining(): Long? {
        val dl = deadline ?: return null
        val days = ChronoUnit.DAYS.between(LocalDate.now(), dl)
        return if (days < 0) 0 else days
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SavingsGoal) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String {
        return "SavingsGoal(id=$id, name='$name', target=$targetAmount, current=$currentAmount)"
    }
}
