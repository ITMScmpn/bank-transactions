package com.banktransactions.features.importing.adapter.in.web.dto;

import com.banktransactions.features.importing.domain.model.ImportJob;
import com.banktransactions.features.importing.domain.model.ImportStatus;

import java.time.LocalDateTime;
import java.util.List;

public record ImportJobResponse(
        String id,
        ImportStatus status,
        String fileName,
        int totalRows,
        int processedRows,
        int validRows,
        int invalidRows,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<TransactionErrorResponse> errors,
        List<String> duplicateTransactionIds
) {
    public record TransactionErrorResponse(int rowNumber, String rawData, String errorMessage) {}

    public static ImportJobResponse from(ImportJob job) {
        List<TransactionErrorResponse> errors = job.getErrors().stream()
                .map(e -> new TransactionErrorResponse(e.rowNumber(), e.rawData(), e.errorMessage()))
                .toList();
        return new ImportJobResponse(
                job.getId(), job.getStatus(), job.getFileName(),
                job.getTotalRows(), job.getProcessedRows(),
                job.getValidRows(), job.getInvalidRows(),
                job.getCreatedAt(), job.getUpdatedAt(),
                errors, job.getDuplicateTransactionIds());
    }
}
