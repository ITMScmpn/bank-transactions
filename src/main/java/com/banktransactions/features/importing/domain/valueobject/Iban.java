package com.banktransactions.features.importing.domain.valueobject;

import java.util.regex.Pattern;

public final class Iban {

    // 2 litery kraju + 2 cyfry kontrolne + min 11 znaków alfanumerycznych = min 15 total
    private static final Pattern IBAN_PATTERN = Pattern.compile("[A-Z]{2}[0-9]{2}[A-Z0-9]{11,30}");

    private final String value;

    public Iban(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("IBAN is required");
        }
        String normalized = raw.trim().toUpperCase().replaceAll("\\s", "");
        if (!IBAN_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Invalid IBAN format: " + raw);
        }
        this.value = normalized;
    }

    public String getValue() { return value; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Iban iban)) return false;
        return value.equals(iban.value);
    }

    @Override
    public int hashCode() { return value.hashCode(); }

    @Override
    public String toString() { return value; }
}

