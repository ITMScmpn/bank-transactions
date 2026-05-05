package com.banktransactions.features.importing.adapter.in.web;

import com.banktransactions.features.importing.adapter.in.web.dto.ImportJobResponse;
import com.banktransactions.features.importing.application.port.in.GetImportStatusUseCase;
import com.banktransactions.features.importing.application.port.in.ListImportsUseCase;
import com.banktransactions.features.importing.application.port.in.UploadTransactionFileUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/imports")
@Tag(name = "Imports", description = "CSV import management")
public class ImportController {

    private final UploadTransactionFileUseCase uploadUseCase;
    private final GetImportStatusUseCase statusUseCase;
    private final ListImportsUseCase listImportsUseCase;

    public ImportController(UploadTransactionFileUseCase uploadUseCase,
                            GetImportStatusUseCase statusUseCase,
                            ListImportsUseCase listImportsUseCase) {
        this.uploadUseCase = uploadUseCase;
        this.statusUseCase = statusUseCase;
        this.listImportsUseCase = listImportsUseCase;
    }

    /**
     * POST /api/v1/imports
     * Przyjmuje plik CSV, tworzy ImportJob i uruchamia async przetwarzanie.
     */
    @Operation(summary = "Upload CSV file with transactions")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Import job created"),
        @ApiResponse(responseCode = "400", description = "Empty file"),
        @ApiResponse(responseCode = "413", description = "File too large (> 50 MB)"),
        @ApiResponse(responseCode = "415", description = "File is not a CSV")
    })
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<ImportJobResponse> upload(
            @RequestParam("file") MultipartFile file) {
        String jobId = uploadUseCase.upload(file);
        return ResponseEntity
                .created(URI.create("/api/v1/imports/" + jobId))
                .body(ImportJobResponse.from(statusUseCase.getStatus(jobId)));
    }

    /**
     * GET /api/v1/imports
     * Zwraca listę wszystkich ImportJobów.
     */
    @Operation(summary = "List all import jobs")
    @ApiResponse(responseCode = "200", description = "List of import jobs")
    @GetMapping
    public ResponseEntity<List<ImportJobResponse>> listAll() {
        List<ImportJobResponse> jobs = listImportsUseCase.listAll().stream()
                .map(ImportJobResponse::from)
                .toList();
        return ResponseEntity.ok(jobs);
    }

    /**
     * GET /api/v1/imports/{id}
     * Zwraca status ImportJob — postęp, liczniki, lista błędów/warningów.
     */
    @Operation(summary = "Get import job status by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Job found"),
        @ApiResponse(responseCode = "404", description = "Job not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ImportJobResponse> getStatus(@PathVariable String id) {
        return ResponseEntity.ok(ImportJobResponse.from(statusUseCase.getStatus(id)));
    }
}
