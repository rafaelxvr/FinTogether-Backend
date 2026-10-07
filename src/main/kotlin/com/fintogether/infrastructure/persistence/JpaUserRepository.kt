package com.fintogether.infrastructure.persistence

import com.fintogether.domain.entity.User
import com.fintogether.domain.repository.UserRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

interface JpaUserRepositoryInterface : JpaRepository<User, UUID> {
    fun findByEmail(email: String): User?
    fun findByResetToken(token: String): User?
    fun existsByEmail(email: String): Boolean
}

@Repository
class JpaUserRepository(
    private val jpaRepository: JpaUserRepositoryInterface
) : UserRepository {

    override fun save(user: User): User = jpaRepository.save(user)

    override fun findById(id: UUID): User? = jpaRepository.findById(id).orElse(null)

    override fun findByEmail(email: String): User? = jpaRepository.findByEmail(email)

    override fun findByResetToken(token: String): User? = jpaRepository.findByResetToken(token)

    override fun existsByEmail(email: String): Boolean = jpaRepository.existsByEmail(email)

    override fun findAll(): List<User> = jpaRepository.findAll()

    override fun delete(user: User) = jpaRepository.delete(user)
}
