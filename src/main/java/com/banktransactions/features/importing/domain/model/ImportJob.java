package com.banktransactions.features.importing.domain.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Agregat domenowy zarządzający cyklem życia importu pliku CSV.
 * <p>State machine:
 * <pre>
 *   PENDING → PROCESSING → COMPLETED
 *                       → COMPLETED_WITH_ERRORS
 *                       → FAILED
 * </pre>
 */
public class ImportJob {

    private final String id;
    private ImportStatus status;
    private final String fileName;
    private int totalRows;
    private int processedRows;
    private int validRows;
    private int invalidRows;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private final List<TransactionError> errors = new ArrayList<>();
    private final List<String> duplicateTransactionIds = new ArrayList<>();

    public ImportJob(String id, String fileName, LocalDateTime createdAt) {
        this.id = id;
        this.fileName = fileName;
        this.status = ImportStatus.PENDING;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    // ── State transitions ──────────────────────────────────────────────────

    public void startProcessing() {
        if (status != ImportStatus.PENDING) {
            throw new IllegalStateException("Job can only start from PENDING state, current: " + status);
        }
        this.status = ImportStatus.PROCESSING;
        this.updatedAt = LocalDateTime.now();
    }

    /** Rejestruje poprawny wiersz bez ostrzeżeń. */
    public void recordValid() {
        processedRows++;
        validRows++;
        updatedAt = LocalDateTime.now();
    }

    /** Rejestruje błąd walidacji — wiersz NIE jest importowany. */
    public void recordInvalid(TransactionError error) {
        processedRows++;
        invalidRows++;
        errors.add(error);
        updatedAt = LocalDateTime.now();
    }

    /**
     * Rejestruje transakcję pominiętą z powodu duplikatu UUID.
     * UUID jest logowany i zbierany w podsumowaniu importu.
     */
    public void recordDuplicate(String transactionId) {
        duplicateTransactionIds.add(transactionId);
        updatedAt = LocalDateTime.now();
    }

    public void complete() {
        this.status = (invalidRows > 0)
                ? ImportStatus.COMPLETED_WITH_ERRORS
                : ImportStatus.COMPLETED;
        updatedAt = LocalDateTime.now();
    }

    public void fail(String reason) {
        this.status = ImportStatus.FAILED;
        errors.add(new TransactionError(0, "", reason));
        updatedAt = LocalDateTime.now();
    }


    public void initializeTotalRows(int totalRows) { this.totalRows = totalRows; }

    // ── Persistence restore ────────────────────────────────────────────────

    public void restoreCounters(int totalRows, int processedRows, int validRows, int invalidRows) {
        this.totalRows = totalRows;
        this.processedRows = processedRows;
        this.validRows = validRows;
        this.invalidRows = invalidRows;
        this.errors.clear();
    }

    public void restoreError(TransactionError error) { this.errors.add(error); }

    public void restoreDuplicate(String transactionId) { this.duplicateTransactionIds.add(transactionId); }

    public void restoreState(ImportStatus restoredStatus) { this.status = restoredStatus; }

    // ── Getters ────────────────────────────────────────────────────────────

    public String getId() { return id; }
    public ImportStatus getStatus() { return status; }
    public String getFileName() { return fileName; }
    public int getTotalRows() { return totalRows; }
    public int getProcessedRows() { return processedRows; }
    public int getValidRows() { return validRows; }
    public int getInvalidRows() { return invalidRows; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public List<TransactionError> getErrors() { return Collections.unmodifiableList(errors); }
    public List<String> getDuplicateTransactionIds() { return Collections.unmodifiableList(duplicateTransactionIds); }
}
