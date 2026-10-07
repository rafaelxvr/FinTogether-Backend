package com.fintogether.domain.entity

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "budgets")
class Budget(
    @Column(name = "name", nullable = false)
    var name: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    val owner: User,

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    var type: BudgetType,

    id: UUID = UUID.randomUUID(),
    createdAt: LocalDateTime = LocalDateTime.now(),
    updatedAt: LocalDateTime = LocalDateTime.now()
) : BaseEntity(id, createdAt, updatedAt) {

    @OneToMany(mappedBy = "budget", cascade = [CascadeType.ALL], orphanRemoval = true)
    private val _members: MutableSet<BudgetMember> = mutableSetOf()
    val members: Set<BudgetMember> get() = _members.toSet()

    fun updateName(newName: String) {
        this.name = newName
    }

    fun changeType(newType: BudgetType) {
        this.type = newType
    }

    fun addMember(user: User, role: MemberRole): BudgetMember {
        val member = BudgetMember(
            budget = this,
            user = user,
            role = role
        )
        _members.add(member)
        return member
    }

    fun removeMember(member: BudgetMember) {
        _members.remove(member)
    }

    fun isOwner(user: User): Boolean = owner.id == user.id

    fun hasMember(user: User): Boolean = _members.any { it.user.id == user.id }

    fun getMemberRole(user: User): MemberRole? = _members.find { it.user.id == user.id }?.role

    override fun toString(): String {
        return "Budget(id=$id, name='$name', type=$type, ownerId=${owner.id})"
    }
}

enum class BudgetType {
    PERSONAL,
    SHARED
}
