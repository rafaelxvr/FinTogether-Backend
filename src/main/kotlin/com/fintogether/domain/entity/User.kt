package com.fintogether.domain.entity

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "users")
class User(
    @Column(name = "email", nullable = false, unique = true)
    var email: String,

    @Column(name = "password_hash")
    var passwordHash: String? = null,

    @Column(name = "name", nullable = false)
    var name: String,

    @Column(name = "locale")
    var locale: String = "pt-BR",

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "settings", columnDefinition = "jsonb")
    var settings: Map<String, Any>? = null,

    @Column(name = "reset_token")
    var resetToken: String? = null,

    @Column(name = "reset_token_expiry")
    var resetTokenExpiry: LocalDateTime? = null,

    id: UUID = UUID.randomUUID(),
    createdAt: LocalDateTime = LocalDateTime.now(),
    updatedAt: LocalDateTime = LocalDateTime.now()
) : BaseEntity(id, createdAt, updatedAt) {

    fun updateProfile(name: String? = null, locale: String? = null, settings: Map<String, Any>? = null) {
        name?.let { this.name = it }
        locale?.let { this.locale = it }
        settings?.let { this.settings = it }
    }

    fun updatePassword(newPasswordHash: String) {
        this.passwordHash = newPasswordHash
    }

    fun isOAuthUser(): Boolean = passwordHash == null

    fun setPasswordResetToken(token: String, expiryHours: Long = 24) {
        this.resetToken = token
        this.resetTokenExpiry = LocalDateTime.now().plusHours(expiryHours)
    }

    fun clearPasswordResetToken() {
        this.resetToken = null
        this.resetTokenExpiry = null
    }

    fun isResetTokenValid(token: String): Boolean {
        if (resetToken == null || resetTokenExpiry == null) {
            return false
        }
        return resetToken == token && LocalDateTime.now().isBefore(resetTokenExpiry)
    }

    override fun toString(): String {
        return "User(id=$id, email='$email', name='$name', locale='$locale')"
    }
}
