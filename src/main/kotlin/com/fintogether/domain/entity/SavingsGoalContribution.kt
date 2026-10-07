package com.fintogether.domain.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "savings_goal_contributions")
class SavingsGoalContribution(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "savings_goal_id", nullable = false)
    val savingsGoal: SavingsGoal,

    @Column(name = "amount", precision = 15, scale = 2, nullable = false)
    val amount: BigDecimal,

    @Column(name = "note", length = 255)
    val note: String? = null,

    @Column(name = "contributed_at", nullable = false)
    val contributedAt: LocalDateTime = LocalDateTime.now(),

    id: UUID = UUID.randomUUID()
) {
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID = id

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SavingsGoalContribution) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String {
        return "SavingsGoalContribution(id=$id, amount=$amount, contributedAt=$contributedAt)"
    }
}
