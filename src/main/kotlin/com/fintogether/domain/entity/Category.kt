package com.fintogether.domain.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "categories")
class Category(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "budget_id", nullable = false)
    val budget: Budget,

    @Column(name = "name", nullable = false)
    var name: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    var type: CategoryType,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    var parent: Category? = null,

    @Column(name = "icon")
    var icon: String? = null,

    @Column(name = "color")
    var color: String? = null,

    @Column(name = "limit_amount", precision = 15, scale = 2)
    var limitAmount: BigDecimal? = null,

    @Column(name = "archived", nullable = false)
    var archived: Boolean = false,

    @Column(name = "is_essential", nullable = false)
    var isEssential: Boolean = false,

    id: UUID = UUID.randomUUID(),
    createdAt: LocalDateTime = LocalDateTime.now()
) {
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID = id

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = createdAt

    @OneToMany(mappedBy = "parent", cascade = [CascadeType.ALL])
    private val _children: MutableSet<Category> = mutableSetOf()
    val children: Set<Category> get() = _children.toSet()

    fun update(
        name: String? = null,
        icon: String? = null,
        color: String? = null,
        limitAmount: BigDecimal? = null,
        isEssential: Boolean? = null
    ) {
        name?.let { this.name = it }
        icon?.let { this.icon = it }
        color?.let { this.color = it }
        this.limitAmount = limitAmount
        isEssential?.let { this.isEssential = it }
    }

    fun archive() {
        this.archived = true
    }

    fun unarchive() {
        this.archived = false
    }

    fun updateParent(parentCategory: Category?) {
        this.parent = parentCategory
    }

    fun isRoot(): Boolean = parent == null

    fun hasLimit(): Boolean = limitAmount != null && limitAmount!! > BigDecimal.ZERO

    fun isIncome(): Boolean = type == CategoryType.INCOME

    fun isExpense(): Boolean = type == CategoryType.EXPENSE

    fun isEssentialExpense(): Boolean = isExpense() && isEssential

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Category) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String {
        return "Category(id=$id, name='$name', type=$type, budgetId=${budget.id})"
    }
}

enum class CategoryType {
    INCOME,
    EXPENSE,
    TRANSFER
}
