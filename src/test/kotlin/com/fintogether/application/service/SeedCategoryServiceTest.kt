package com.fintogether.application.service

import com.fintogether.domain.entity.Budget
import com.fintogether.domain.entity.BudgetType
import com.fintogether.domain.entity.Category
import com.fintogether.domain.entity.CategoryType
import com.fintogether.domain.entity.User
import com.fintogether.domain.repository.CategoryRepository
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

class SeedCategoryServiceTest {

    private lateinit var categoryRepository: CategoryRepository
    private lateinit var seedCategoryService: SeedCategoryService

    private lateinit var budget: Budget
    private lateinit var owner: User

    @BeforeEach
    fun setUp() {
        categoryRepository = mockk()
        seedCategoryService = SeedCategoryService(categoryRepository)

        owner = User(
            email = "owner@example.com",
            passwordHash = "hash",
            name = "Test User",
            id = UUID.randomUUID()
        )

        budget = Budget(
            name = "Test Budget",
            owner = owner,
            type = BudgetType.PERSONAL,
            id = UUID.randomUUID()
        )

        every { categoryRepository.saveAll(any<List<Category>>()) } answers { firstArg() }
    }

    @Test
    fun `seedDefaultCategories with pt-BR locale creates 10 categories with correct names`() {
        // Given
        val capturedCategories = slot<List<Category>>()
        every { categoryRepository.saveAll(capture(capturedCategories)) } answers { firstArg() }

        // When
        seedCategoryService.seedDefaultCategories(budget, "pt-BR")

        // Then
        val categories = capturedCategories.captured
        assertEquals(10, categories.size)

        val names = categories.map { it.name }
        assertTrue(names.contains("Alimentacao"))
        assertTrue(names.contains("Transporte"))
        assertTrue(names.contains("Moradia"))
        assertTrue(names.contains("Saude"))
        assertTrue(names.contains("Educacao"))
        assertTrue(names.contains("Lazer"))
        assertTrue(names.contains("Salario"))
        assertTrue(names.contains("Freelance"))
        assertTrue(names.contains("Investimentos"))
        assertTrue(names.contains("Outros"))

        // Verify types
        val expenseCategories = categories.filter { it.type == CategoryType.EXPENSE }
        val incomeCategories = categories.filter { it.type == CategoryType.INCOME }
        assertEquals(7, expenseCategories.size)
        assertEquals(3, incomeCategories.size)

        // Verify essential flags
        val essentialCategories = categories.filter { it.isEssential }
        assertEquals(5, essentialCategories.size)
        assertTrue(essentialCategories.all { it.type == CategoryType.EXPENSE })
    }

    @Test
    fun `seedDefaultCategories with pt locale falls back to pt-BR templates`() {
        // Given
        val capturedCategories = slot<List<Category>>()
        every { categoryRepository.saveAll(capture(capturedCategories)) } answers { firstArg() }

        // When
        seedCategoryService.seedDefaultCategories(budget, "pt")

        // Then
        val categories = capturedCategories.captured
        assertEquals(10, categories.size)
        assertTrue(categories.any { it.name == "Alimentacao" })
    }

    @Test
    fun `seedDefaultCategories with en locale creates 10 categories with English names`() {
        // Given
        val capturedCategories = slot<List<Category>>()
        every { categoryRepository.saveAll(capture(capturedCategories)) } answers { firstArg() }

        // When
        seedCategoryService.seedDefaultCategories(budget, "en")

        // Then
        val categories = capturedCategories.captured
        assertEquals(10, categories.size)

        val names = categories.map { it.name }
        assertTrue(names.contains("Food"))
        assertTrue(names.contains("Transport"))
        assertTrue(names.contains("Housing"))
        assertTrue(names.contains("Health"))
        assertTrue(names.contains("Education"))
        assertTrue(names.contains("Entertainment"))
        assertTrue(names.contains("Salary"))
        assertTrue(names.contains("Freelance"))
        assertTrue(names.contains("Investments"))
        assertTrue(names.contains("Other"))
    }

    @Test
    fun `seedDefaultCategories with unknown locale defaults to English`() {
        // Given
        val capturedCategories = slot<List<Category>>()
        every { categoryRepository.saveAll(capture(capturedCategories)) } answers { firstArg() }

        // When
        seedCategoryService.seedDefaultCategories(budget, "fr-FR")

        // Then
        val categories = capturedCategories.captured
        assertEquals(10, categories.size)
        assertTrue(categories.any { it.name == "Food" })
        assertFalse(categories.any { it.name == "Alimentacao" })
    }

    @Test
    fun `seedDefaultCategories assigns correct icons and colors`() {
        // Given
        val capturedCategories = slot<List<Category>>()
        every { categoryRepository.saveAll(capture(capturedCategories)) } answers { firstArg() }

        // When
        seedCategoryService.seedDefaultCategories(budget, "en")

        // Then
        val categories = capturedCategories.captured
        val food = categories.first { it.name == "Food" }
        assertEquals("restaurant", food.icon)
        assertEquals("#4CAF50", food.color)

        val transport = categories.first { it.name == "Transport" }
        assertEquals("directions_car", transport.icon)
        assertEquals("#2196F3", transport.color)
    }

    @Test
    fun `seedDefaultCategories associates all categories with the given budget`() {
        // Given
        val capturedCategories = slot<List<Category>>()
        every { categoryRepository.saveAll(capture(capturedCategories)) } answers { firstArg() }

        // When
        seedCategoryService.seedDefaultCategories(budget, "en")

        // Then
        val categories = capturedCategories.captured
        assertTrue(categories.all { it.budget.id == budget.id })
    }

    @Test
    fun `seedDefaultCategories calls saveAll exactly once`() {
        // When
        seedCategoryService.seedDefaultCategories(budget, "en")

        // Then
        verify(exactly = 1) { categoryRepository.saveAll(any<List<Category>>()) }
    }
}
