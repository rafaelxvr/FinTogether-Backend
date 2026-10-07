package com.fintogether.domain.repository

import com.fintogether.domain.entity.User
import java.util.UUID

interface UserRepository {
    fun save(user: User): User
    fun findById(id: UUID): User?
    fun findByEmail(email: String): User?
    fun findByResetToken(token: String): User?
    fun existsByEmail(email: String): Boolean
    fun findAll(): List<User>
    fun delete(user: User)
}
