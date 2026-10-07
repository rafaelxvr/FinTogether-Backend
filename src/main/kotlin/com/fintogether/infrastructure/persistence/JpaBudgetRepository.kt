package com.fintogether.infrastructure.persistence

import com.fintogether.domain.entity.Budget
import com.fintogether.domain.entity.User
import com.fintogether.domain.repository.BudgetRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

interface JpaBudgetRepositoryInterface : JpaRepository<Budget, UUID> {
    fun findByOwner(owner: User): List<Budget>
    fun findByOwnerId(ownerId: UUID): List<Budget>
    fun findByOwnerId(ownerId: UUID, pageable: Pageable): Page<Budget>
}

@Repository
class JpaBudgetRepository(
    private val jpaRepository: JpaBudgetRepositoryInterface
) : BudgetRepository {

    override fun save(budget: Budget): Budget = jpaRepository.save(budget)

    override fun findById(id: UUID): Budget? = jpaRepository.findById(id).orElse(null)

    override fun findByOwner(owner: User): List<Budget> = jpaRepository.findByOwner(owner)

    override fun findByOwnerId(ownerId: UUID): List<Budget> = jpaRepository.findByOwnerId(ownerId)

    override fun findByOwnerId(ownerId: UUID, pageable: Pageable): Page<Budget> =
        jpaRepository.findByOwnerId(ownerId, pageable)

    override fun findAll(): List<Budget> = jpaRepository.findAll()

    override fun delete(budget: Budget) = jpaRepository.delete(budget)
}
