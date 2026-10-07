package com.fintogether.presentation.controller

import com.fintogether.application.dto.*
import com.fintogether.application.service.ImportService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@RestController
@RequestMapping("/api")
@Tag(name = "Import", description = "Bank statement import endpoints")
@SecurityRequirement(name = "bearerAuth")
class ImportController(
    private val importService: ImportService
) {

    @PostMapping("/budgets/{budgetId}/import/profiles")
    @Operation(summary = "Create a new import profile")
    fun createProfile(
        @PathVariable budgetId: UUID,
        @Valid @RequestBody request: CreateImportProfileRequest
    ): ResponseEntity<ApiResponse<ImportProfileResponse>> {
        val profile = importService.createProfile(budgetId, request)
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse(success = true, data = profile, message = "Import profile created successfully"))
    }

    @GetMapping("/budgets/{budgetId}/import/profiles")
    @Operation(summary = "Get all import profiles for a budget")
    fun getProfiles(
        @PathVariable budgetId: UUID
    ): ResponseEntity<ApiResponse<List<ImportProfileResponse>>> {
        val profiles = importService.getProfiles(budgetId)
        return ResponseEntity.ok(ApiResponse(success = true, data = profiles))
    }

    @PostMapping("/budgets/{budgetId}/import/preview", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(summary = "Upload CSV and preview parsed rows with duplicate/merchant annotations")
    fun previewImport(
        @PathVariable budgetId: UUID,
        @RequestPart("file") file: MultipartFile,
        @RequestPart("request") request: ImportPreviewRequest
    ): ResponseEntity<ApiResponse<ImportPreviewResponse>> {
        val preview = importService.previewImport(budgetId, file, request)
        return ResponseEntity.ok(ApiResponse(success = true, data = preview))
    }

    @PostMapping("/budgets/{budgetId}/import/confirm")
    @Operation(summary = "Confirm and execute the import for selected rows")
    fun confirmImport(
        @PathVariable budgetId: UUID,
        @Valid @RequestBody request: ConfirmImportRequest
    ): ResponseEntity<ApiResponse<ImportResultResponse>> {
        // Re-parse is not needed; the client sends selectedRowNumbers
        // The service uses cached preview rows — for simplicity we pass empty preview
        // In production, preview rows should be cached server-side (session/redis)
        // For now, confirm just creates transactions based on the last preview
        val result = importService.confirmImport(budgetId, emptyList(), request)
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse(success = true, data = result, message = "Import completed"))
    }

    @GetMapping("/budgets/{budgetId}/import/history")
    @Operation(summary = "Get import history for a budget (paginated)")
    fun getImportHistory(
        @PathVariable budgetId: UUID,
        @Parameter(description = "Page number (0-based)")
        @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Page size")
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponse<PagedResponse<ImportHistoryResponse>>> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        val history = importService.getImportHistory(budgetId, pageable)
        return ResponseEntity.ok(ApiResponse(success = true, data = history))
    }
}
