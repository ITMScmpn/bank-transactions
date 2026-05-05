package com.banktransactions.features.importing.domain.service;

import com.banktransactions.features.importing.domain.model.TransactionCategory;
import com.banktransactions.features.importing.domain.valueobject.Iban;
import com.banktransactions.features.importing.domain.valueobject.Money;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Serwis domenowy do walidacji pól transakcji.
 * Logika domenowa jest wolna od Springa; adnotacja @Service jest jedynym
 * wyjątkiem — pozwala Springowi wstrzyknąć serwis bez naruszania reguł domeny.
 */
@Service
public class TransactionValidator {

    public Iban validateAndParseIban(String raw) {
        return new Iban(raw);
    }

    public LocalDate validateAndParseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Transaction date is required");
        }
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format (expected yyyy-MM-dd): " + raw);
        }
    }

    public Money validateAndParseMoney(String amountRaw, String currencyRaw) {
        return Money.of(amountRaw, currencyRaw);
    }

    public TransactionCategory parseCategory(String raw) {
        return TransactionCategory.parse(raw);
    }
}
