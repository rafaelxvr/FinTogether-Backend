package com.fintogether.application.service

import com.fintogether.application.dto.*
import com.fintogether.application.mapper.ImportProfileMapper
import com.fintogether.domain.entity.*
import com.fintogether.domain.exception.AccessDeniedException
import com.fintogether.domain.exception.EntityNotFoundException
import com.fintogether.domain.exception.InvalidOperationException
import com.fintogether.domain.repository.*
import com.fintogether.infrastructure.security.UserPrincipal
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.BufferedReader
import java.io.InputStreamReader
import java.math.BigDecimal
import java.nio.charset.Charset
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.UUID

@Service
class ImportService(
    private val importProfileRepository: ImportProfileRepository,
    private val importHistoryRepository: ImportHistoryRepository,
    private val transactionRepository: TransactionRepository,
    private val merchantRepository: MerchantRepository,
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository,
    private val userRepository: UserRepository,
    private val accountRepository: AccountRepository,
    private val importProfileMapper: ImportProfileMapper
) {

    private val logger = LoggerFactory.getLogger(ImportService::class.java)

    @Transactional
    fun createProfile(budgetId: UUID, request: CreateImportProfileRequest): ImportProfileResponse {
        logger.info("Creating import profile '${request.name}' for budget: $budgetId")
        val budget = findBudgetAndValidateAccess(budgetId)

        val profile = ImportProfile(
            budget = budget,
            name = request.name,
            delimiter = request.delimiter,
            encoding = request.encoding,
            mapping = request.mapping
        )

        val saved = importProfileRepository.save(profile)
        logger.info("Import profile created: ${saved.id}")
        return importProfileMapper.toResponse(saved)
    }

    @Transactional(readOnly = true)
    fun getProfiles(budgetId: UUID): List<ImportProfileResponse> {
        logger.debug("Getting import profiles for budget: $budgetId")
        validateBudgetAccess(budgetId)
        return importProfileRepository.findByBudgetId(budgetId)
            .map { importProfileMapper.toResponse(it) }
    }

    @Transactional(readOnly = true)
    fun previewImport(
        budgetId: UUID,
        file: MultipartFile,
        request: ImportPreviewRequest
    ): ImportPreviewResponse {
        logger.info("Previewing import for budget: $budgetId, file: ${file.originalFilename}")
        validateBudgetAccess(budgetId)

        // Resolve mapping from profile or inline
        val mapping: Map<String, Any>
        val delimiter: String
        val encoding: String
        var profileName: String? = null

        if (request.profileId != null) {
            val profile = importProfileRepository.findById(request.profileId)
                ?: throw EntityNotFoundException("ImportProfile", request.profileId)
            mapping = profile.mapping
            delimiter = profile.delimiter
            encoding = profile.encoding
            profileName = profile.name
        } else if (request.mapping != null) {
            mapping = request.mapping
            delimiter = mapping["delimiter"]?.toString() ?: ","
            encoding = mapping["encoding"]?.toString() ?: "UTF-8"
        } else {
            throw InvalidOperationException("Either profileId or inline mapping is required", "previewImport")
        }

        // Parse CSV
        val rows = parseCsv(file, delimiter, encoding, mapping)

        // Get existing transactions for duplicate detection
        val existingTransactions = transactionRepository.findByBudgetId(budgetId)

        // Get merchants for matching
        val merchants = merchantRepository.findByBudgetId(budgetId)

        // Get categories for auto-assignment
        val categories = categoryRepository.findByBudgetId(budgetId)

        // Annotate rows with duplicates and merchant matches
        val previewRows = rows.mapIndexed { index, parsedRow ->
            val isDuplicate = existingTransactions.any { tx ->
                tx.transactionDate == parsedRow.date &&
                    tx.amount.compareTo(parsedRow.amount) == 0 &&
                    tx.description == parsedRow.description
            }

            val duplicateTxId = if (isDuplicate) {
                existingTransactions.firstOrNull { tx ->
                    tx.transactionDate == parsedRow.date &&
                        tx.amount.compareTo(parsedRow.amount) == 0 &&
                        tx.description == parsedRow.description
                }?.id
            } else null

            // Merchant matching by description contains
            val matchedMerchant = parsedRow.description?.let { desc ->
                merchants.firstOrNull { merchant ->
                    desc.contains(merchant.name, ignoreCase = true) ||
                        (merchant.displayName != null && desc.contains(merchant.displayName!!, ignoreCase = true))
                }
            }

            // Category from matched merchant's default category
            val matchedCategory = matchedMerchant?.defaultCategory

            ImportPreviewRow(
                rowNumber = index + 1,
                date = parsedRow.date,
                description = parsedRow.description,
                amount = parsedRow.amount,
                type = parsedRow.type,
                matchedMerchantId = matchedMerchant?.id,
                matchedMerchantName = matchedMerchant?.effectiveDisplayName(),
                matchedCategoryId = matchedCategory?.id,
                matchedCategoryName = matchedCategory?.name,
                isDuplicate = isDuplicate,
                duplicateTransactionId = duplicateTxId,
                selected = !isDuplicate
            )
        }

        val duplicateCount = previewRows.count { it.isDuplicate }

        return ImportPreviewResponse(
            rows = previewRows,
            totalRows = previewRows.size,
            duplicateCount = duplicateCount,
            profileUsed = profileName
        )
    }

    @Transactional
    fun confirmImport(
        budgetId: UUID,
        previewRows: List<ImportPreviewRow>,
        request: ConfirmImportRequest
    ): ImportResultResponse {
        logger.info("Confirming import for budget: $budgetId, selected: ${request.selectedRowNumbers.size} rows")
        val budget = findBudgetAndValidateAccess(budgetId)
        val currentUser = getCurrentUser()
        val user = userRepository.findById(currentUser.id)
            ?: throw EntityNotFoundException("User", currentUser.id)

        val account = request.accountId?.let { accountId ->
            val acc = accountRepository.findById(accountId)
                ?: throw EntityNotFoundException("Account", accountId)
            if (acc.budget.id != budgetId) {
                throw InvalidOperationException("Account must belong to the same budget", "confirmImport")
            }
            acc
        }

        val selectedRows = previewRows.filter { it.rowNumber in request.selectedRowNumbers }
        var importedCount = 0
        var skippedCount = 0

        for (row in selectedRows) {
            try {
                if (row.date == null || row.amount == null) {
                    skippedCount++
                    continue
                }

                val type = when (row.type?.uppercase()) {
                    "INCOME" -> TransactionType.INCOME
                    "TRANSFER" -> TransactionType.TRANSFER
                    else -> TransactionType.EXPENSE
                }

                // Look up merchant and category
                val merchant = row.matchedMerchantId?.let { merchantRepository.findById(it) }
                val category = row.matchedCategoryId?.let { categoryRepository.findById(it) }

                val transaction = Transaction(
                    budget = budget,
                    user = user,
                    transactionDate = row.date,
                    amount = row.amount,
                    type = type,
                    account = account,
                    category = category,
                    description = row.description,
                    merchant = merchant
                )

                transactionRepository.save(transaction)

                // Update account balance
                if (account != null) {
                    when (type) {
                        TransactionType.INCOME -> account.addAmount(row.amount)
                        TransactionType.EXPENSE -> account.subtractAmount(row.amount)
                        TransactionType.TRANSFER -> {}
                    }
                    accountRepository.save(account)
                }

                importedCount++
            } catch (e: Exception) {
                logger.warn("Failed to import row ${row.rowNumber}: ${e.message}")
                skippedCount++
            }
        }

        // Resolve profile if provided
        val profile = request.profileId?.let { importProfileRepository.findById(it) }

        val status = when {
            importedCount == 0 -> ImportStatus.FAILED
            skippedCount > 0 -> ImportStatus.PARTIAL
            else -> ImportStatus.COMPLETED
        }

        val history = ImportHistory(
            budget = budget,
            importProfile = profile,
            fileName = request.fileName ?: "import.csv",
            totalRows = previewRows.size,
            importedRows = importedCount,
            skippedRows = skippedCount,
            duplicateRows = previewRows.count { it.isDuplicate },
            status = status,
            importedBy = user
        )

        val savedHistory = importHistoryRepository.save(history)
        logger.info("Import completed: $importedCount imported, $skippedCount skipped")

        return ImportResultResponse(
            totalImported = importedCount,
            totalSkipped = skippedCount,
            importHistoryId = savedHistory.id
        )
    }

    @Transactional(readOnly = true)
    fun getImportHistory(budgetId: UUID, pageable: Pageable): PagedResponse<ImportHistoryResponse> {
        logger.debug("Getting import history for budget: $budgetId")
        validateBudgetAccess(budgetId)
        val page = importHistoryRepository.findByBudgetId(budgetId, pageable)
        return PagedResponse.from(page) { importProfileMapper.toHistoryResponse(it) }
    }

    // --- CSV Parsing ---

    private data class ParsedRow(
        val date: LocalDate?,
        val description: String?,
        val amount: BigDecimal?,
        val type: String?
    )

    private fun parseCsv(
        file: MultipartFile,
        delimiter: String,
        encoding: String,
        mapping: Map<String, Any>
    ): List<ParsedRow> {
        val charset = try {
            Charset.forName(encoding)
        } catch (_: Exception) {
            Charsets.UTF_8
        }

        val dateColIndex = (mapping["date"] as? Number)?.toInt()
        val descriptionColIndex = (mapping["description"] as? Number)?.toInt()
        val amountColIndex = (mapping["amount"] as? Number)?.toInt()
        val typeColIndex = (mapping["type"] as? Number)?.toInt()
        val dateFormat = mapping["dateFormat"]?.toString() ?: "yyyy-MM-dd"
        val hasHeader = mapping["hasHeader"] as? Boolean ?: true

        val formatter = DateTimeFormatter.ofPattern(dateFormat)
        val delimiterChar = delimiter.firstOrNull() ?: ','

        val rows = mutableListOf<ParsedRow>()

        BufferedReader(InputStreamReader(file.inputStream, charset)).use { reader ->
            var lineNumber = 0
            reader.forEachLine { line ->
                lineNumber++
                // Skip header row
                if (lineNumber == 1 && hasHeader) return@forEachLine
                if (line.isBlank()) return@forEachLine

                val columns = parseCsvLine(line, delimiterChar)

                val date = dateColIndex?.let { idx ->
                    if (idx < columns.size) {
                        try {
                            LocalDate.parse(columns[idx].trim(), formatter)
                        } catch (_: DateTimeParseException) {
                            null
                        }
                    } else null
                }

                val description = descriptionColIndex?.let { idx ->
                    if (idx < columns.size) columns[idx].trim() else null
                }

                val rawAmount = amountColIndex?.let { idx ->
                    if (idx < columns.size) {
                        try {
                            val cleaned = columns[idx].trim()
                                .replace("[^\\d.,-]".toRegex(), "")
                                .replace(",", ".")
                            BigDecimal(cleaned).abs()
                        } catch (_: Exception) {
                            null
                        }
                    } else null
                }

                val type = if (typeColIndex != null && typeColIndex < columns.size) {
                    columns[typeColIndex].trim()
                } else {
                    // Infer from amount sign in raw value
                    amountColIndex?.let { idx ->
                        if (idx < columns.size) {
                            val raw = columns[idx].trim().replace("[^\\d.,-]".toRegex(), "")
                            if (raw.startsWith("-")) "EXPENSE" else "INCOME"
                        } else "EXPENSE"
                    } ?: "EXPENSE"
                }

                rows.add(ParsedRow(date = date, description = description, amount = rawAmount, type = type))
            }
        }

        return rows
    }

    private fun parseCsvLine(line: String, delimiter: Char): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false

        for (char in line) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char == delimiter && !inQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }
                else -> current.append(char)
            }
        }
        result.add(current.toString())
        return result
    }

    // --- Helpers ---

    private fun findBudgetAndValidateAccess(budgetId: UUID): Budget {
        val budget = budgetRepository.findById(budgetId)
            ?: throw EntityNotFoundException("Budget", budgetId)
        validateBudgetAccess(budgetId)
        return budget
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
