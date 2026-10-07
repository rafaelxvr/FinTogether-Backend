package com.fintogether.domain.entity

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "import_history")
class ImportHistory(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "budget_id", nullable = false)
    val budget: Budget,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "import_profile_id")
    var importProfile: ImportProfile? = null,

    @Column(name = "file_name", nullable = false, length = 500)
    val fileName: String,

    @Column(name = "total_rows", nullable = false)
    val totalRows: Int,

    @Column(name = "imported_rows", nullable = false)
    val importedRows: Int,

    @Column(name = "skipped_rows", nullable = false)
    val skippedRows: Int,

    @Column(name = "duplicate_rows", nullable = false)
    val duplicateRows: Int = 0,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    val status: ImportStatus,

    @Column(name = "error_message", columnDefinition = "text")
    val errorMessage: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "imported_by", nullable = false)
    val importedBy: User,

    id: UUID = UUID.randomUUID(),
    createdAt: LocalDateTime = LocalDateTime.now()
) {
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID = id

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = createdAt

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ImportHistory) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String {
        return "ImportHistory(id=$id, fileName='$fileName', status=$status, imported=$importedRows/$totalRows)"
    }
}

enum class ImportStatus {
    COMPLETED,
    PARTIAL,
    FAILED
}
