package com.fintogether.application.service

import com.fintogether.application.dto.*
import com.fintogether.application.mapper.MerchantMapper
import com.fintogether.domain.entity.Merchant
import com.fintogether.domain.exception.AccessDeniedException
import com.fintogether.domain.exception.EntityNotFoundException
import com.fintogether.domain.exception.InvalidOperationException
import com.fintogether.domain.repository.BudgetRepository
import com.fintogether.domain.repository.CategoryRepository
import com.fintogether.domain.repository.MerchantRepository
import com.fintogether.domain.repository.TransactionRepository
import com.fintogether.domain.repository.UserRepository
import com.fintogether.infrastructure.security.UserPrincipal
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.UUID

@Service
class MerchantService(
    private val merchantRepository: MerchantRepository,
    private val budgetRepository: BudgetRepository,
    private val userRepository: UserRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val merchantMapper: MerchantMapper
) {

    private val logger = LoggerFactory.getLogger(MerchantService::class.java)

    @Transactional
    fun createMerchant(budgetId: UUID, request: CreateMerchantRequest): MerchantResponse {
        logger.info("Creating merchant '${request.name}' for budget: $budgetId")

        val budget = budgetRepository.findById(budgetId)
            ?: throw EntityNotFoundException("Budget", budgetId)

        validateBudgetAccess(budget.id)

        // Check for duplicate name
        val existing = merchantRepository.findByBudgetIdAndName(budgetId, request.name)
        if (existing != null) {
            throw InvalidOperationException(
                "A merchant with name '${request.name}' already exists in this budget",
                "createMerchant"
            )
        }

        // Validate default category if provided
        val defaultCategory = request.defaultCategoryId?.let { categoryId ->
            val cat = categoryRepository.findById(categoryId)
                ?: throw EntityNotFoundException("Category", categoryId)
            if (cat.budget.id != budgetId) {
                throw InvalidOperationException(
                    "Default category must belong to the same budget",
                    "createMerchant"
                )
            }
            cat
        }

        val merchant = Merchant(
            budget = budget,
            name = request.name,
            displayName = request.displayName,
            logoUrl = request.logoUrl,
            color = request.color,
            defaultCategory = defaultCategory
        )

        val savedMerchant = merchantRepository.save(merchant)
        logger.info("Merchant created successfully: ${savedMerchant.id}")
        return merchantMapper.toResponse(savedMerchant)
    }

    @Transactional(readOnly = true)
    fun getMerchantById(id: UUID): MerchantResponse {
        logger.debug("Getting merchant by id: $id")
        val merchant = findMerchantById(id)
        validateBudgetAccess(merchant.budget.id)
        return merchantMapper.toResponse(merchant)
    }

    @Transactional(readOnly = true)
    fun getMerchantsByBudget(budgetId: UUID): List<MerchantResponse> {
        logger.debug("Getting merchants for budget: $budgetId")
        validateBudgetAccess(budgetId)
        val merchants = merchantRepository.findByBudgetId(budgetId)
        return merchants.map { merchantMapper.toResponse(it) }
    }

    @Transactional(readOnly = true)
    fun getMerchantsByBudget(budgetId: UUID, pageable: Pageable): PagedResponse<MerchantResponse> {
        logger.debug("Getting paginated merchants for budget: $budgetId, page: ${pageable.pageNumber}, size: ${pageable.pageSize}")
        validateBudgetAccess(budgetId)
        val page = merchantRepository.findByBudgetId(budgetId, pageable)
        return PagedResponse.from(page) { merchantMapper.toResponse(it) }
    }

    @Transactional(readOnly = true)
    fun searchMerchants(budgetId: UUID, query: String): List<MerchantResponse> {
        logger.debug("Searching merchants for budget: $budgetId, query: $query")
        validateBudgetAccess(budgetId)
        val merchants = merchantRepository.findByBudgetIdAndNameContainingIgnoreCase(budgetId, query)
        return merchants.map { merchantMapper.toResponse(it) }
    }

    @Transactional
    fun updateMerchant(id: UUID, request: UpdateMerchantRequest): MerchantResponse {
        logger.info("Updating merchant: $id")
        val merchant = findMerchantById(id)
        validateBudgetAccess(merchant.budget.id)

        // Check for duplicate name (excluding current merchant)
        val existing = merchantRepository.findByBudgetIdAndName(merchant.budget.id, request.name)
        if (existing != null && existing.id != id) {
            throw InvalidOperationException(
                "A merchant with name '${request.name}' already exists in this budget",
                "updateMerchant"
            )
        }

        // Validate default category if provided
        val defaultCategory = request.defaultCategoryId?.let { categoryId ->
            val cat = categoryRepository.findById(categoryId)
                ?: throw EntityNotFoundException("Category", categoryId)
            if (cat.budget.id != merchant.budget.id) {
                throw InvalidOperationException(
                    "Default category must belong to the same budget",
                    "updateMerchant"
                )
            }
            cat
        }

        merchant.update(
            name = request.name,
            displayName = request.displayName,
            logoUrl = request.logoUrl,
            color = request.color,
            defaultCategory = defaultCategory
        )

        val updatedMerchant = merchantRepository.save(merchant)
        logger.info("Merchant updated successfully: $id")
        return merchantMapper.toResponse(updatedMerchant)
    }

    @Transactional
    fun deleteMerchant(id: UUID) {
        logger.info("Deleting merchant: $id")
        val merchant = findMerchantById(id)
        validateBudgetAccess(merchant.budget.id)

        merchantRepository.delete(merchant)
        logger.info("Merchant deleted successfully: $id")
    }

    @Transactional
    fun archiveMerchant(id: UUID): MerchantResponse {
        logger.info("Archiving merchant: $id")
        val merchant = findMerchantById(id)
        validateBudgetAccess(merchant.budget.id)

        merchant.archive()
        val updatedMerchant = merchantRepository.save(merchant)
        logger.info("Merchant archived successfully: $id")
        return merchantMapper.toResponse(updatedMerchant)
    }

    @Transactional
    fun unarchiveMerchant(id: UUID): MerchantResponse {
        logger.info("Unarchiving merchant: $id")
        val merchant = findMerchantById(id)
        validateBudgetAccess(merchant.budget.id)

        merchant.unarchive()
        val updatedMerchant = merchantRepository.save(merchant)
        logger.info("Merchant unarchived successfully: $id")
        return merchantMapper.toResponse(updatedMerchant)
    }

    @Transactional(readOnly = true)
    fun getTopMerchants(
        budgetId: UUID,
        startDate: LocalDate,
        endDate: LocalDate,
        limit: Int = 5
    ): TopMerchantsResponse {
        logger.debug("Getting top merchants for budget: $budgetId, period: $startDate to $endDate")
        validateBudgetAccess(budgetId)

        val transactions = transactionRepository.findByBudgetIdAndDateBetween(budgetId, startDate, endDate)

        // Group by merchant, calculate totals
        val merchantTotals = transactions
            .filter { it.merchant != null && it.isExpense() }
            .groupBy { it.merchant!! }
            .map { (merchant, txns) ->
                Triple(
                    merchant,
                    txns.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) },
                    txns.size.toLong()
                )
            }
            .sortedByDescending { it.second }
            .take(limit)

        val totalSpend = merchantTotals.fold(BigDecimal.ZERO) { acc, triple -> acc.add(triple.second) }

        val summaries = merchantTotals.map { (merchant, total, count) ->
            val percentage = if (totalSpend > BigDecimal.ZERO) {
                total.multiply(BigDecimal(100)).divide(totalSpend, 2, RoundingMode.HALF_UP)
            } else {
                BigDecimal.ZERO
            }

            MerchantSpendingSummary(
                merchantId = merchant.id,
                merchantName = merchant.effectiveDisplayName(),
                totalAmount = total,
                transactionCount = count,
                percentage = percentage
            )
        }

        val period = "$startDate to $endDate"
        return TopMerchantsResponse(
            period = period,
            summaries = summaries,
            totalSpend = totalSpend
        )
    }

    private fun findMerchantById(id: UUID): Merchant {
        return merchantRepository.findById(id)
            ?: throw EntityNotFoundException("Merchant", id)
    }

    private fun validateBudgetAccess(budgetId: UUID) {
        val currentUser = getCurrentUser()
        val user = userRepository.findById(currentUser.id)
            ?: throw EntityNotFoundException("User", currentUser.id)

        val budget = budgetRepository.findById(budgetId)
            ?: throw EntityNotFoundException("Budget", budgetId)

        if (!budget.hasMember(user) && !budget.isOwner(user)) {
            logger.warn("Access denied for user ${currentUser.id} to budget $budgetId")
            throw AccessDeniedException("You don't have access to this budget", "Budget", budgetId)
        }
    }

    private fun getCurrentUser(): UserPrincipal {
        val authentication = SecurityContextHolder.getContext().authentication
        return authentication.principal as UserPrincipal
    }
}
