package com.fintogether.application.service

import com.fintogether.application.dto.*
import com.fintogether.application.mapper.BudgetMapper
import com.fintogether.domain.entity.Budget
import com.fintogether.domain.entity.BudgetType
import com.fintogether.domain.entity.MemberRole
import com.fintogether.domain.entity.User
import com.fintogether.domain.exception.AccessDeniedException
import com.fintogether.domain.exception.EntityNotFoundException
import com.fintogether.domain.repository.BudgetRepository
import com.fintogether.domain.repository.UserRepository
import com.fintogether.infrastructure.security.UserPrincipal
import io.mockk.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import java.time.LocalDateTime
import java.util.*

class BudgetServiceTest {

    private lateinit var budgetRepository: BudgetRepository
    private lateinit var userRepository: UserRepository
    private lateinit var budgetMapper: BudgetMapper
    private lateinit var seedCategoryService: SeedCategoryService
    private lateinit var budgetService: BudgetService

    private lateinit var securityContext: SecurityContext
    private lateinit var authentication: Authentication

    private val ownerId = UUID.randomUUID()
    private val budgetId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        budgetRepository = mockk()
        userRepository = mockk()
        budgetMapper = mockk()
        seedCategoryService = mockk(relaxed = true)

        budgetService = BudgetService(
            budgetRepository = budgetRepository,
            userRepository = userRepository,
            budgetMapper = budgetMapper,
            seedCategoryService = seedCategoryService
        )

        // Mock SecurityContext
        securityContext = mockk()
        authentication = mockk()
        SecurityContextHolder.setContext(securityContext)

        every { securityContext.authentication } returns authentication
        every { authentication.principal } returns UserPrincipal(
            ownerId,
            "owner@example.com",
            "password",
            listOf(SimpleGrantedAuthority("ROLE_USER"))
        )
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
        SecurityContextHolder.clearContext()
    }

    // ==================== CREATE BUDGET TESTS ====================

    @Test
    fun `createBudget should create new budget and add owner as member`() {
        // Given
        val request = CreateBudgetRequest(
            name = "My Budget",
            type = BudgetType.PERSONAL
        )

        val owner = createUser(ownerId, "owner@example.com")
        val budget = createBudget(budgetId, "My Budget", owner)
        val now = LocalDateTime.now()

        val budgetResponse = BudgetResponse(
            id = budget.id,
            name = budget.name,
            ownerId = owner.id,
            ownerName = owner.name,
            type = budget.type,
            memberCount = 1,
            createdAt = now,
            updatedAt = now
        )

        every { userRepository.findById(ownerId) } returns owner
        every { budgetRepository.save(any<Budget>()) } returns budget
        every { budgetMapper.toResponse(budget) } returns budgetResponse

        // When
        val response = budgetService.createBudget(request)

        // Then
        assertNotNull(response)
        assertEquals(budgetResponse.id, response.id)
        assertEquals(budgetResponse.name, response.name)
        assertEquals(budgetResponse.ownerId, response.ownerId)

        verify(exactly = 1) { userRepository.findById(ownerId) }
        verify(exactly = 1) { budgetRepository.save(any<Budget>()) }
        verify(exactly = 1) { budgetMapper.toResponse(budget) }
    }

    @Test
    fun `createBudget should seed default categories after budget creation`() {
        // Given
        val request = CreateBudgetRequest(
            name = "My Budget",
            type = BudgetType.PERSONAL
        )

        val owner = createUser(ownerId, "owner@example.com")
        val budget = createBudget(budgetId, "My Budget", owner)
        val now = LocalDateTime.now()

        val budgetResponse = BudgetResponse(
            id = budget.id,
            name = budget.name,
            ownerId = owner.id,
            ownerName = owner.name,
            type = budget.type,
            memberCount = 1,
            createdAt = now,
            updatedAt = now
        )

        every { userRepository.findById(ownerId) } returns owner
        every { budgetRepository.save(any<Budget>()) } returns budget
        every { budgetMapper.toResponse(budget) } returns budgetResponse

        // When
        budgetService.createBudget(request)

        // Then
        verify(exactly = 1) { seedCategoryService.seedDefaultCategories(budget, owner.locale) }
    }

    @Test
    fun `createBudget should use request locale when provided`() {
        // Given
        val request = CreateBudgetRequest(
            name = "My Budget",
            type = BudgetType.PERSONAL,
            locale = "en"
        )

        val owner = createUser(ownerId, "owner@example.com")
        val budget = createBudget(budgetId, "My Budget", owner)
        val now = LocalDateTime.now()

        val budgetResponse = BudgetResponse(
            id = budget.id,
            name = budget.name,
            ownerId = owner.id,
            ownerName = owner.name,
            type = budget.type,
            memberCount = 1,
            createdAt = now,
            updatedAt = now
        )

        every { userRepository.findById(ownerId) } returns owner
        every { budgetRepository.save(any<Budget>()) } returns budget
        every { budgetMapper.toResponse(budget) } returns budgetResponse

        // When
        budgetService.createBudget(request)

        // Then
        verify(exactly = 1) { seedCategoryService.seedDefaultCategories(budget, "en") }
    }

    @Test
    fun `createBudget should throw EntityNotFoundException when user not found`() {
        // Given
        val request = CreateBudgetRequest(
            name = "My Budget",
            type = BudgetType.PERSONAL
        )

        every { userRepository.findById(ownerId) } returns null

        // When & Then
        assertThrows<EntityNotFoundException> {
            budgetService.createBudget(request)
        }

        verify(exactly = 1) { userRepository.findById(ownerId) }
        verify(exactly = 0) { budgetRepository.save(any<Budget>()) }
    }

    // ==================== GET BUDGET BY ID TESTS ====================

    @Test
    fun `getBudgetById should return budget when user has access`() {
        // Given
        val owner = createUser(ownerId, "owner@example.com")
        val budget = createBudget(budgetId, "My Budget", owner)
        budget.addMember(owner, MemberRole.OWNER)
        val now = LocalDateTime.now()

        val budgetResponse = BudgetResponse(
            id = budget.id,
            name = budget.name,
            ownerId = owner.id,
            ownerName = owner.name,
            type = budget.type,
            memberCount = 1,
            createdAt = now,
            updatedAt = now
        )

        every { budgetRepository.findById(budgetId) } returns budget
        every { userRepository.findById(ownerId) } returns owner
        every { budgetMapper.toResponse(budget) } returns budgetResponse

        // When
        val response = budgetService.getBudgetById(budgetId)

        // Then
        assertNotNull(response)
        assertEquals(budgetResponse.id, response.id)
        assertEquals(budgetResponse.name, response.name)

        verify(exactly = 1) { budgetRepository.findById(budgetId) }
        verify(exactly = 1) { userRepository.findById(ownerId) }
        verify(exactly = 1) { budgetMapper.toResponse(budget) }
    }

    @Test
    fun `getBudgetById should throw EntityNotFoundException when budget not found`() {
        // Given
        every { budgetRepository.findById(budgetId) } returns null

        // When & Then
        assertThrows<EntityNotFoundException> {
            budgetService.getBudgetById(budgetId)
        }

        verify(exactly = 1) { budgetRepository.findById(budgetId) }
        verify(exactly = 0) { budgetMapper.toResponse(any()) }
    }

    @Test
    fun `getBudgetById should throw AccessDeniedException when user has no access`() {
        // Given
        val otherOwnerId = UUID.randomUUID()
        val otherOwner = createUser(otherOwnerId, "other@example.com")
        val currentUser = createUser(ownerId, "owner@example.com")
        val budget = createBudget(budgetId, "Other Budget", otherOwner)

        every { budgetRepository.findById(budgetId) } returns budget
        every { userRepository.findById(ownerId) } returns currentUser

        // When & Then
        assertThrows<AccessDeniedException> {
            budgetService.getBudgetById(budgetId)
        }
    }

    // ==================== GET ALL BUDGETS TESTS ====================

    @Test
    fun `getAllBudgets should return all budgets for current user`() {
        // Given
        val owner = createUser(ownerId, "owner@example.com")
        val budget1 = createBudget(UUID.randomUUID(), "Budget 1", owner)
        val budget2 = createBudget(UUID.randomUUID(), "Budget 2", owner)

        every { budgetRepository.findByOwnerId(ownerId) } returns listOf(budget1, budget2)
        every { budgetMapper.toResponse(any()) } returns mockk()

        // When
        val response = budgetService.getAllBudgets()

        // Then
        assertEquals(2, response.size)
        verify(exactly = 1) { budgetRepository.findByOwnerId(ownerId) }
        verify(exactly = 2) { budgetMapper.toResponse(any()) }
    }

    // ==================== UPDATE BUDGET TESTS ====================

    @Test
    fun `updateBudget should update budget when user is owner`() {
        // Given
        val owner = createUser(ownerId, "owner@example.com")
        val budget = createBudget(budgetId, "Old Name", owner)

        val request = UpdateBudgetRequest(
            name = "New Name",
            type = BudgetType.SHARED
        )

        every { budgetRepository.findById(budgetId) } returns budget
        every { userRepository.findById(ownerId) } returns owner
        every { budgetRepository.save(any<Budget>()) } returns budget
        every { budgetMapper.toResponse(any()) } returns mockk()

        // When
        budgetService.updateBudget(budgetId, request)

        // Then
        verify(exactly = 1) { budgetRepository.save(any<Budget>()) }
    }

    @Test
    fun `updateBudget should throw AccessDeniedException when user is not owner`() {
        // Given
        val otherOwner = createUser(UUID.randomUUID(), "other@example.com")
        val currentUser = createUser(ownerId, "owner@example.com")
        val budget = createBudget(budgetId, "Budget", otherOwner)
        budget.addMember(currentUser, MemberRole.MEMBER)

        val request = UpdateBudgetRequest(
            name = "New Name",
            type = BudgetType.SHARED
        )

        every { budgetRepository.findById(budgetId) } returns budget
        every { userRepository.findById(ownerId) } returns currentUser

        // When & Then
        assertThrows<AccessDeniedException> {
            budgetService.updateBudget(budgetId, request)
        }
    }

    // ==================== DELETE BUDGET TESTS ====================

    @Test
    fun `deleteBudget should delete budget when user is owner`() {
        // Given
        val owner = createUser(ownerId, "owner@example.com")
        val budget = createBudget(budgetId, "My Budget", owner)

        every { budgetRepository.findById(budgetId) } returns budget
        every { userRepository.findById(ownerId) } returns owner
        every { budgetRepository.delete(budget) } just Runs

        // When
        budgetService.deleteBudget(budgetId)

        // Then
        verify(exactly = 1) { budgetRepository.findById(budgetId) }
        verify(exactly = 1) { userRepository.findById(ownerId) }
        verify(exactly = 1) { budgetRepository.delete(budget) }
    }

    // ==================== ADD MEMBER TESTS ====================

    @Test
    fun `addMember should add new member to budget`() {
        // Given
        val owner = createUser(ownerId, "owner@example.com")
        val budget = createBudget(budgetId, "My Budget", owner)
        val newUser = createUser(UUID.randomUUID(), "member@example.com")

        val request = BudgetMemberRequest(
            email = "member@example.com",
            role = "MEMBER"
        )

        every { budgetRepository.findById(budgetId) } returns budget
        every { userRepository.findById(ownerId) } returns owner
        every { userRepository.findByEmail("member@example.com") } returns newUser
        every { budgetRepository.save(any<Budget>()) } returns budget
        every { budgetMapper.toMemberResponse(any()) } returns mockk()

        // When
        budgetService.addMember(budgetId, request)

        // Then
        verify(exactly = 1) { userRepository.findByEmail("member@example.com") }
        verify(exactly = 1) { budgetRepository.save(any<Budget>()) }
    }

    @Test
    fun `addMember should throw when user is already a member`() {
        // Given
        val owner = createUser(ownerId, "owner@example.com")
        val budget = createBudget(budgetId, "My Budget", owner)
        val existingMember = createUser(UUID.randomUUID(), "member@example.com")
        budget.addMember(existingMember, MemberRole.MEMBER)

        val request = BudgetMemberRequest(
            email = "member@example.com",
            role = "MEMBER"
        )

        every { budgetRepository.findById(budgetId) } returns budget
        every { userRepository.findById(ownerId) } returns owner
        every { userRepository.findByEmail("member@example.com") } returns existingMember

        // When & Then
        assertThrows<IllegalArgumentException> {
            budgetService.addMember(budgetId, request)
        }
    }

    // ==================== HELPER METHODS ====================

    private fun createUser(id: UUID, email: String): User {
        return User(
            email = email,
            passwordHash = "hash",
            name = "Test User",
            locale = "en-US",
            id = id
        )
    }

    private fun createBudget(id: UUID, name: String, owner: User): Budget {
        return Budget(
            name = name,
            owner = owner,
            type = BudgetType.PERSONAL,
            id = id
        )
    }
}
