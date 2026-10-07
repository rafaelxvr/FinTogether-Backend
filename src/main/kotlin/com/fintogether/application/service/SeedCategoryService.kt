package com.fintogether.application.service

import com.fintogether.domain.entity.Budget
import com.fintogether.domain.entity.Category
import com.fintogether.domain.entity.CategoryType
import com.fintogether.domain.repository.CategoryRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SeedCategoryService(
    private val categoryRepository: CategoryRepository
) {

    private val logger = LoggerFactory.getLogger(SeedCategoryService::class.java)

    fun seedDefaultCategories(budget: Budget, locale: String = "pt-BR") {
        val templates = when (locale.lowercase()) {
            "pt-br", "pt" -> ptBrTemplates()
            else -> enTemplates()
        }

        val categories = templates.map { template ->
            Category(
                budget = budget,
                name = template.name,
                type = template.type,
                icon = template.icon,
                color = template.color,
                isEssential = template.isEssential
            )
        }

        categoryRepository.saveAll(categories)
        logger.info("Seeded ${categories.size} default categories for budget ${budget.id} (locale: $locale)")
    }

    private fun ptBrTemplates(): List<CategoryTemplate> = listOf(
        CategoryTemplate("Alimentacao", CategoryType.EXPENSE, "restaurant", "#4CAF50", true),
        CategoryTemplate("Transporte", CategoryType.EXPENSE, "directions_car", "#2196F3", true),
        CategoryTemplate("Moradia", CategoryType.EXPENSE, "home", "#795548", true),
        CategoryTemplate("Saude", CategoryType.EXPENSE, "local_hospital", "#F44336", true),
        CategoryTemplate("Educacao", CategoryType.EXPENSE, "school", "#9C27B0", true),
        CategoryTemplate("Lazer", CategoryType.EXPENSE, "sports_esports", "#FF9800", false),
        CategoryTemplate("Salario", CategoryType.INCOME, "account_balance_wallet", "#4CAF50", false),
        CategoryTemplate("Freelance", CategoryType.INCOME, "laptop", "#00BCD4", false),
        CategoryTemplate("Investimentos", CategoryType.INCOME, "trending_up", "#8BC34A", false),
        CategoryTemplate("Outros", CategoryType.EXPENSE, "more_horiz", "#9E9E9E", false)
    )

    private fun enTemplates(): List<CategoryTemplate> = listOf(
        CategoryTemplate("Food", CategoryType.EXPENSE, "restaurant", "#4CAF50", true),
        CategoryTemplate("Transport", CategoryType.EXPENSE, "directions_car", "#2196F3", true),
        CategoryTemplate("Housing", CategoryType.EXPENSE, "home", "#795548", true),
        CategoryTemplate("Health", CategoryType.EXPENSE, "local_hospital", "#F44336", true),
        CategoryTemplate("Education", CategoryType.EXPENSE, "school", "#9C27B0", true),
        CategoryTemplate("Entertainment", CategoryType.EXPENSE, "sports_esports", "#FF9800", false),
        CategoryTemplate("Salary", CategoryType.INCOME, "account_balance_wallet", "#4CAF50", false),
        CategoryTemplate("Freelance", CategoryType.INCOME, "laptop", "#00BCD4", false),
        CategoryTemplate("Investments", CategoryType.INCOME, "trending_up", "#8BC34A", false),
        CategoryTemplate("Other", CategoryType.EXPENSE, "more_horiz", "#9E9E9E", false)
    )

    private data class CategoryTemplate(
        val name: String,
        val type: CategoryType,
        val icon: String,
        val color: String,
        val isEssential: Boolean
    )
}
