package com.banktransactions.features.importing.domain.service;

import com.banktransactions.features.importing.domain.model.TransactionCategory;
import com.banktransactions.features.importing.domain.valueobject.Iban;
import com.banktransactions.features.importing.domain.valueobject.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

class TransactionValidatorTest {

    private TransactionValidator validator;

    @BeforeEach
    void setUp() { validator = new TransactionValidator(); }

    @Test
    void shouldParseValidIban() {
        Iban iban = validator.validateAndParseIban("PL61109010140000071219812874");
        assertThat(iban.getValue()).isEqualTo("PL61109010140000071219812874");
    }

    @Test
    void shouldRejectInvalidIban() {
        assertThatThrownBy(() -> validator.validateAndParseIban("NOTANIBAN"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldParseValidDate() {
        LocalDate date = validator.validateAndParseDate("2024-03-15");
        assertThat(date).isEqualTo(LocalDate.of(2024, 3, 15));
    }

    @Test
    void shouldRejectInvalidDateFormat() {
        assertThatThrownBy(() -> validator.validateAndParseDate("15/03/2024"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid date format");
    }

    @Test
    void shouldRejectBlankDate() {
        assertThatThrownBy(() -> validator.validateAndParseDate(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("required");
    }

    @Test
    void shouldParseValidMoney() {
        Money money = validator.validateAndParseMoney("199.99", "PLN");
        assertThat(money.getAmount()).isEqualByComparingTo("199.99");
        assertThat(money.getCurrency()).isEqualTo("PLN");
    }

    @Test
    void shouldRejectInvalidAmount() {
        assertThatThrownBy(() -> validator.validateAndParseMoney("not-a-number", "PLN"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldAcceptZeroAmount() {
        // Zero jest poprawną wartością — wiersz importowany normalnie
        Money money = validator.validateAndParseMoney("0.00", "PLN");
        assertThat(money.getAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    void shouldAcceptValidCategory() {
        assertThat(validator.parseCategory("GROCERIES")).isEqualTo(TransactionCategory.GROCERIES);
    }

    @Test
    void shouldAcceptCategoryIgnoringCase() {
        assertThat(validator.parseCategory("groceries")).isEqualTo(TransactionCategory.GROCERIES);
    }

    @Test
    void shouldRejectBlankCategory() {
        assertThatThrownBy(() -> validator.parseCategory("  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Category is required");
    }

    @Test
    void shouldRejectUnknownCategory() {
        assertThatThrownBy(() -> validator.parseCategory("UNKNOWN_CAT"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown category");
    }
}
