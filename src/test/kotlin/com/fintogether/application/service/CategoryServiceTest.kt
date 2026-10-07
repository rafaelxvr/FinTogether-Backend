package com.fintogether.application.service

import com.fintogether.application.dto.CreateCategoryRequest
import com.fintogether.application.dto.UpdateCategoryRequest
import com.fintogether.application.mapper.CategoryMapper
import com.fintogether.domain.entity.*
import com.fintogether.domain.exception.AccessDeniedException
import com.fintogether.domain.exception.EntityNotFoundException
import com.fintogether.domain.exception.InvalidOperationException
import com.fintogether.domain.repository.BudgetRepository
import com.fintogether.domain.repository.CategoryRepository
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
import java.math.BigDecimal
import java.util.*

class CategoryServiceTest {

    private lateinit var categoryRepository: CategoryRepository
    private lateinit var budgetRepository: BudgetRepository
    private lateinit var userRepository: UserRepository
    private lateinit var categoryMapper: CategoryMapper
    private lateinit var categoryService: CategoryService

    private lateinit var securityContext: SecurityContext
    private lateinit var authentication: Authentication

    private val userId = UUID.randomUUID()
    private val budgetId = UUID.randomUUID()
    private val categoryId = UUID.randomUUID()

    private lateinit var owner: User
    private lateinit var budget: Budget

    @BeforeEach
    fun setUp() {
        categoryRepository = mockk()
        budgetRepository = mockk()
        userRepository = mockk()
        categoryMapper = mockk()

        categoryService = CategoryService(
            categoryRepository = categoryRepository,
            budgetRepository = budgetRepository,
            userRepository = userRepository,
            categoryMapper = categoryMapper
        )

        // Mock SecurityContext
        securityContext = mockk()
        authentication = mockk()
        SecurityContextHolder.setContext(securityContext)

        every { securityContext.authentication } returns authentication
        every { authentication.principal } returns UserPrincipal(
            userId,
            "user@example.com",
            "password",
            listOf(SimpleGrantedAuthority("ROLE_USER"))
        )

        owner = createUser(userId, "user@example.com")
        budget = createBudget(budgetId, "My Budget", owner)
        budget.addMember(owner, MemberRole.OWNER)
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
        SecurityContextHolder.clearContext()
    }

    // ==================== CREATE CATEGORY TESTS ====================

    @Test
    fun `createCategory should create root category when parentId is null`() {
        // Given
        val request = CreateCategoryRequest(
            name = "Food",
            type = CategoryType.EXPENSE,
            parentId = null,
            icon = "food",
            color = "#FF5722",
            limitAmount = BigDecimal("500.00")
        )

        val category = Category(
            budget = budget,
            name = request.name,
            type = request.type,
            icon = request.icon,
            color = request.color,
            limitAmount = request.limitAmount,
            id = categoryId
        )

        every { budgetRepository.findById(budgetId) } returns budget
        every { userRepository.findById(userId) } returns owner
        every { categoryRepository.save(any<Category>()) } returns category
        every { categoryMapper.toResponse(category) } returns mockk()

        // When
        categoryService.createCategory(budgetId, request)

        // Then
        verify(exactly = 1) { budgetRepository.findById(budgetId) }
        verify(exactly = 1) { categoryRepository.save(any<Category>()) }
    }

    @Test
    fun `createCategory should create subcategory with valid parent`() {
        // Given
        val parentId = UUID.randomUUID()
        val parentCategory = Category(
            budget = budget,
            name = "Food",
            type = CategoryType.EXPENSE,
            id = parentId
        )

        val request = CreateCategoryRequest(
            name = "Groceries",
            type = CategoryType.EXPENSE,
            parentId = parentId,
            icon = "cart",
            color = "#FF5722",
            limitAmount = null
        )

        val category = Category(
            budget = budget,
            name = request.name,
            type = request.type,
            parent = parentCategory,
            id = categoryId
        )

        every { budgetRepository.findById(budgetId) } returns budget
        every { userRepository.findById(userId) } returns owner
        every { categoryRepository.findById(parentId) } returns parentCategory
        every { categoryRepository.save(any<Category>()) } returns category
        every { categoryMapper.toResponse(category) } returns mockk()

        // When
        categoryService.createCategory(budgetId, request)

        // Then
        verify(exactly = 1) { categoryRepository.findById(parentId) }
        verify(exactly = 1) { categoryRepository.save(any<Category>()) }
    }

    @Test
    fun `createCategory should throw EntityNotFoundException when budget not found`() {
        // Given
        val request = CreateCategoryRequest(
            name = "Food",
            type = CategoryType.EXPENSE,
            parentId = null,
            icon = "food",
            color = "#FF5722",
            limitAmount = null
        )

        every { budgetRepository.findById(budgetId) } returns null

        // When & Then
        assertThrows<EntityNotFoundException> {
            categoryService.createCategory(budgetId, request)
        }

        verify(exactly = 0) { categoryRepository.save(any<Category>()) }
    }

    @Test
    fun `createCategory should throw InvalidOperationException when parent type mismatches`() {
        // Given
        val parentId = UUID.randomUUID()
        val incomeParent = Category(
            budget = budget,
            name = "Salary",
            type = CategoryType.INCOME,
            id = parentId
        )

        val request = CreateCategoryRequest(
            name = "Groceries",
            type = CategoryType.EXPENSE,
            parentId = parentId,
            icon = "cart",
            color = "#FF5722",
            limitAmount = null
        )

        every { budgetRepository.findById(budgetId) } returns budget
        every { userRepository.findById(userId) } returns owner
        every { categoryRepository.findById(parentId) } returns incomeParent

        // When & Then
        val exception = assertThrows<InvalidOperationException> {
            categoryService.createCategory(budgetId, request)
        }

        assertEquals("Parent category must have the same type", exception.message)
    }

    @Test
    fun `createCategory should throw InvalidOperationException when parent belongs to different budget`() {
        // Given
        val otherBudget = createBudget(UUID.randomUUID(), "Other Budget", owner)
        val parentId = UUID.randomUUID()
        val parentCategory = Category(
            budget = otherBudget,
            name = "Food",
            type = CategoryType.EXPENSE,
            id = parentId
        )

        val request = CreateCategoryRequest(
            name = "Groceries",
            type = CategoryType.EXPENSE,
            parentId = parentId,
            icon = "cart",
            color = "#FF5722",
            limitAmount = null
        )

        every { budgetRepository.findById(budgetId) } returns budget
        every { userRepository.findById(userId) } returns owner
        every { categoryRepository.findById(parentId) } returns parentCategory

        // When & Then
        val exception = assertThrows<InvalidOperationException> {
            categoryService.createCategory(budgetId, request)
        }

        assertEquals("Parent category must belong to the same budget", exception.message)
    }

    // ==================== GET CATEGORY TESTS ====================

    @Test
    fun `getCategoryById should return category when user has access`() {
        // Given
        val category = Category(
            budget = budget,
            name = "Food",
            type = CategoryType.EXPENSE,
            id = categoryId
        )

        every { categoryRepository.findById(categoryId) } returns category
        every { budgetRepository.findById(budgetId) } returns budget
        every { userRepository.findById(userId) } returns owner
        every { categoryMapper.toResponse(category) } returns mockk()

        // When
        categoryService.getCategoryById(categoryId)

        // Then
        verify(exactly = 1) { categoryRepository.findById(categoryId) }
    }

    @Test
    fun `getCategoryById should throw EntityNotFoundException when category not found`() {
        // Given
        every { categoryRepository.findById(categoryId) } returns null

        // When & Then
        assertThrows<EntityNotFoundException> {
            categoryService.getCategoryById(categoryId)
        }

        verify(exactly = 1) { categoryRepository.findById(categoryId) }
    }

    @Test
    fun `getCategoriesByBudget should filter by type when provided`() {
        // Given
        val categories = listOf(
            Category(budget = budget, name = "Food", type = CategoryType.EXPENSE, id = UUID.randomUUID()),
            Category(budget = budget, name = "Transport", type = CategoryType.EXPENSE, id = UUID.randomUUID())
        )

        every { budgetRepository.findById(budgetId) } returns budget
        every { userRepository.findById(userId) } returns owner
        every { categoryRepository.findByBudgetIdAndType(budgetId, CategoryType.EXPENSE) } returns categories
        every { categoryMapper.toResponse(any()) } returns mockk()

        // When
        val result = categoryService.getCategoriesByBudget(budgetId, CategoryType.EXPENSE)

        // Then
        assertEquals(2, result.size)
        verify(exactly = 1) { categoryRepository.findByBudgetIdAndType(budgetId, CategoryType.EXPENSE) }
    }

    // ==================== UPDATE CATEGORY TESTS ====================

    @Test
    fun `updateCategory should throw InvalidOperationException when category is its own parent`() {
        // Given
        val category = Category(
            budget = budget,
            name = "Food",
            type = CategoryType.EXPENSE,
            id = categoryId
        )

        val request = UpdateCategoryRequest(
            name = "Food",
            parentId = categoryId
        )

        every { categoryRepository.findById(categoryId) } returns category
        every { budgetRepository.findById(budgetId) } returns budget
        every { userRepository.findById(userId) } returns owner

        // When & Then
        val exception = assertThrows<InvalidOperationException> {
            categoryService.updateCategory(categoryId, request)
        }

        assertEquals("Category cannot be its own parent", exception.message)
    }

    // ==================== DELETE CATEGORY TESTS ====================

    @Test
    fun `deleteCategory should delete category without subcategories`() {
        // Given
        val category = Category(
            budget = budget,
            name = "Food",
            type = CategoryType.EXPENSE,
            id = categoryId
        )

        every { categoryRepository.findById(categoryId) } returns category
        every { budgetRepository.findById(budgetId) } returns budget
        every { userRepository.findById(userId) } returns owner
        every { categoryRepository.delete(category) } just Runs

        // When
        categoryService.deleteCategory(categoryId)

        // Then
        verify(exactly = 1) { categoryRepository.delete(category) }
    }

    @Test
    fun `deleteCategory should throw InvalidOperationException when category has subcategories`() {
        // Given
        val parentCategory = Category(
            budget = budget,
            name = "Food",
            type = CategoryType.EXPENSE,
            id = categoryId
        )

        // Add a child
        val child = Category(
            budget = budget,
            name = "Groceries",
            type = CategoryType.EXPENSE,
            parent = parentCategory,
            id = UUID.randomUUID()
        )
        parentCategory.children.add(child)

        every { categoryRepository.findById(categoryId) } returns parentCategory
        every { budgetRepository.findById(budgetId) } returns budget
        every { userRepository.findById(userId) } returns owner

        // When & Then
        val exception = assertThrows<InvalidOperationException> {
            categoryService.deleteCategory(categoryId)
        }

        assertEquals("Cannot delete category with subcategories", exception.message)
        verify(exactly = 0) { categoryRepository.delete(any()) }
    }

    // ==================== ARCHIVE/UNARCHIVE TESTS ====================

    @Test
    fun `archiveCategory should archive category`() {
        // Given
        val category = Category(
            budget = budget,
            name = "Food",
            type = CategoryType.EXPENSE,
            id = categoryId
        )

        every { categoryRepository.findById(categoryId) } returns category
        every { budgetRepository.findById(budgetId) } returns budget
        every { userRepository.findById(userId) } returns owner
        every { categoryRepository.save(any<Category>()) } returns category
        every { categoryMapper.toResponse(any()) } returns mockk()

        // When
        categoryService.archiveCategory(categoryId)

        // Then
        verify(exactly = 1) { categoryRepository.save(any<Category>()) }
    }

    @Test
    fun `unarchiveCategory should unarchive category`() {
        // Given
        val category = Category(
            budget = budget,
            name = "Food",
            type = CategoryType.EXPENSE,
            id = categoryId
        )

        every { categoryRepository.findById(categoryId) } returns category
        every { budgetRepository.findById(budgetId) } returns budget
        every { userRepository.findById(userId) } returns owner
        every { categoryRepository.save(any<Category>()) } returns category
        every { categoryMapper.toResponse(any()) } returns mockk()

        // When
        categoryService.unarchiveCategory(categoryId)

        // Then
        verify(exactly = 1) { categoryRepository.save(any<Category>()) }
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
