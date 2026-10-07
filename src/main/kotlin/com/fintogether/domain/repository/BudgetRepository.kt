package com.fintogether.domain.repository

import com.fintogether.domain.entity.Budget
import com.fintogether.domain.entity.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

interface BudgetRepository {
    fun save(budget: Budget): Budget
    fun findById(id: UUID): Budget?
    fun findByOwner(owner: User): List<Budget>
    fun findByOwnerId(ownerId: UUID): List<Budget>
    fun findByOwnerId(ownerId: UUID, pageable: Pageable): Page<Budget>
    fun findAll(): List<Budget>
    fun delete(budget: Budget)
}
