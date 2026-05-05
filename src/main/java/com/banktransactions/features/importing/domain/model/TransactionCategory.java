package com.banktransactions.features.importing.domain.model;

/**
 * Dozwolone kategorie transakcji.
 * Walidacja na poziomie domeny — nieznana kategoria w CSV → błąd wiersza.
 */
public enum TransactionCategory {
    GROCERIES,
    TRANSPORT,
    ENTERTAINMENT,
    UTILITIES,
    DINING,
    HEALTHCARE,
    SHOPPING,
    TRAVEL,
    EDUCATION,
    SALARY,
    OTHER;

    /**
     * Parsuje string (case-insensitive) do enum.
     * @throws IllegalArgumentException gdy kategoria nieznana
     */
    public static TransactionCategory parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Category is required");
        }
        try {
            return valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Unknown category: '" + raw.trim() + "'. Allowed values: " + allowedValues());
        }
    }

    private static String allowedValues() {
        StringBuilder sb = new StringBuilder();
        for (TransactionCategory c : values()) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(c.name());
        }
        return sb.toString();
    }
}

