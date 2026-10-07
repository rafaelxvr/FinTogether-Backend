package com.fintogether.domain.entity

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "budget_members",
    uniqueConstraints = [UniqueConstraint(name = "uk_budget_user", columnNames = ["budget_id", "user_id"])]
)
class BudgetMember(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "budget_id", nullable = false)
    val budget: Budget,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    var role: MemberRole,

    @Column(name = "joined_at", nullable = false)
    val joinedAt: LocalDateTime = LocalDateTime.now(),

    id: UUID = UUID.randomUUID()
) {
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID = id

    fun updateRole(newRole: MemberRole) {
        this.role = newRole
    }

    fun canEdit(): Boolean = role in listOf(MemberRole.OWNER, MemberRole.MEMBER)

    fun canView(): Boolean = true

    fun isOwner(): Boolean = role == MemberRole.OWNER

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BudgetMember) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String {
        return "BudgetMember(id=$id, budgetId=${budget.id}, userId=${user.id}, role=$role)"
    }
}

enum class MemberRole {
    OWNER,
    MEMBER,
    VIEWER
}
