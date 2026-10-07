package com.fintogether.domain.entity

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "import_profiles")
class ImportProfile(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "budget_id", nullable = false)
    val budget: Budget,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id")
    var card: CreditCard? = null,

    @Column(name = "name", nullable = false)
    var name: String,

    @Column(name = "delimiter", nullable = false)
    var delimiter: String = ",",

    @Column(name = "encoding", nullable = false)
    var encoding: String = "UTF-8",

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "mapping", columnDefinition = "jsonb", nullable = false)
    var mapping: Map<String, Any>,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rules", columnDefinition = "jsonb")
    var rules: Map<String, Any>? = null,

    id: UUID = UUID.randomUUID(),
    createdAt: LocalDateTime = LocalDateTime.now()
) {
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID = id

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = createdAt

    fun update(
        name: String? = null,
        delimiter: String? = null,
        encoding: String? = null,
        mapping: Map<String, Any>? = null,
        rules: Map<String, Any>? = null
    ) {
        name?.let { this.name = it }
        delimiter?.let { this.delimiter = it }
        encoding?.let { this.encoding = it }
        mapping?.let { this.mapping = it }
        rules?.let { this.rules = it }
    }

    fun linkCard(card: CreditCard?) {
        this.card = card
    }

    fun hasRules(): Boolean = rules != null && rules!!.isNotEmpty()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ImportProfile) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String {
        return "ImportProfile(id=$id, name='$name', budgetId=${budget.id})"
    }
}
