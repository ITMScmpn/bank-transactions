package com.banktransactions.features.importing.domain.valueobject;

import java.math.BigDecimal;
import java.util.regex.Pattern;

public final class Money {

    private static final Pattern CURRENCY_PATTERN = Pattern.compile("[A-Z]{3}");

    private final BigDecimal amount;
    private final String currency;

    public Money(BigDecimal amount, String currency) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount is required");
        }
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Currency is required");
        }
        String normalized = currency.trim().toUpperCase();
        if (!CURRENCY_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Invalid currency format (expected ISO 4217, e.g. PLN): " + currency);
        }
        this.amount = amount;
        this.currency = normalized;
    }

    public static Money of(String amountStr, String currency) {
        if (amountStr == null || amountStr.isBlank()) {
            throw new IllegalArgumentException("Amount is required");
        }
        try {
            return new Money(new BigDecimal(amountStr.trim()), currency);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid amount format: " + amountStr);
        }
    }

    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money money)) return false;
        return amount.compareTo(money.amount) == 0 && currency.equals(money.currency);
    }

    @Override
    public int hashCode() { return 31 * amount.hashCode() + currency.hashCode(); }

    @Override
    public String toString() { return amount + " " + currency; }
}

