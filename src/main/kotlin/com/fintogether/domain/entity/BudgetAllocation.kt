package com.fintogether.domain.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "budget_allocations",
    uniqueConstraints = [UniqueConstraint(columnNames = ["budget_id", "category_id", "month"])]
)
class BudgetAllocation(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "budget_id", nullable = false)
    val budget: Budget,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    val category: Category,

    @Column(name = "month", nullable = false)
    val month: LocalDate,

    @Column(name = "allocated_amount", nullable = false, precision = 15, scale = 2)
    var allocatedAmount: BigDecimal,

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

    fun updateAmount(newAmount: BigDecimal) {
        require(newAmount >= BigDecimal.ZERO) { "Allocation amount must be non-negative" }
        this.allocatedAmount = newAmount
        this.updatedAt = LocalDateTime.now()
    }

    fun validateAgainstLimit() {
        val limit = category.limitAmount
        if (limit != null) {
            require(allocatedAmount <= limit) {
                "Allocation (${allocatedAmount}) cannot exceed category limit (${limit})"
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BudgetAllocation) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String {
        return "BudgetAllocation(id=$id, budgetId=${budget.id}, categoryId=${category.id}, month=$month, allocatedAmount=$allocatedAmount)"
    }
}
