package com.fintogether.domain.entity

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "merchants")
class Merchant(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "budget_id", nullable = false)
    val budget: Budget,

    @Column(name = "name", nullable = false)
    var name: String,

    @Column(name = "display_name")
    var displayName: String? = null,

    @Column(name = "logo_url")
    var logoUrl: String? = null,

    @Column(name = "color")
    var color: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_category_id")
    var defaultCategory: Category? = null,

    @Column(name = "archived", nullable = false)
    var archived: Boolean = false,

    id: UUID = UUID.randomUUID(),
    createdAt: LocalDateTime = LocalDateTime.now(),
    updatedAt: LocalDateTime = LocalDateTime.now()
) : BaseEntity(id, createdAt, updatedAt) {

    fun update(
        name: String? = null,
        displayName: String? = null,
        logoUrl: String? = null,
        color: String? = null,
        defaultCategory: Category? = null
    ) {
        name?.let { this.name = it }
        this.displayName = displayName
        this.logoUrl = logoUrl
        this.color = color
        this.defaultCategory = defaultCategory
    }

    fun archive() {
        this.archived = true
    }

    fun unarchive() {
        this.archived = false
    }

    fun isActive(): Boolean = !archived

    fun effectiveDisplayName(): String = displayName ?: name

    override fun toString(): String {
        return "Merchant(id=$id, name='$name', budgetId=${budget.id})"
    }
}
