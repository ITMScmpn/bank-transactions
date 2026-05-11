package com.banktransactions.features.importing.domain.exception;

/**
 * Rzucany gdy plik CSV przekracza maksymalną liczbę obsługiwanych wierszy.
 * Powoduje przejście ImportJob w stan FAILED.
 */
public class TooManyRowsException extends RuntimeException {
    private final int actual;
    private final int limit;

    public TooManyRowsException(int actual, int limit) {
        super("CSV file has " + actual + " data rows, which exceeds the maximum allowed limit of " + limit + ".");
        this.actual = actual;
        this.limit = limit;
    }

    public int getActual() { return actual; }
    public int getLimit() { return limit; }
}

