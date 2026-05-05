package com.banktransactions.infrastructure.exception;

import java.util.Set;

/**
 * Rzucany gdy plik CSV nie zawiera wymaganych nagłówków kolumn.
 * Powoduje przejście ImportJob w stan FAILED.
 */
public class InvalidCsvStructureException extends RuntimeException {
    private final Set<String> missingColumns;

    public InvalidCsvStructureException(Set<String> missingColumns) {
        super("Invalid CSV structure. Missing required columns: " + missingColumns
              + ". Expected: transactionId, iban, transactionDate, currency, category, amount.");
        this.missingColumns = missingColumns;
    }

    public Set<String> getMissingColumns() { return missingColumns; }
}

