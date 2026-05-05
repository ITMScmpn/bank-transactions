package com.banktransactions.features.importing.domain.valueobject;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class MoneyTest {

    @Test
    void shouldCreateFromValidValues() {
        Money money = new Money(new BigDecimal("150.50"), "PLN");
        assertThat(money.getAmount()).isEqualByComparingTo("150.50");
        assertThat(money.getCurrency()).isEqualTo("PLN");
    }

    @Test
    void shouldAcceptNegativeAmount() {
        Money money = Money.of("-45.00", "EUR");
        assertThat(money.getAmount()).isEqualByComparingTo("-45.00");
    }

    @Test
    void shouldNormalizeCurrencyToUpperCase() {
        Money money = new Money(BigDecimal.TEN, "pln");
        assertThat(money.getCurrency()).isEqualTo("PLN");
    }

    @Test
    void shouldRejectInvalidCurrencyFormat() {
        assertThatThrownBy(() -> new Money(BigDecimal.ONE, "pl"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid currency format");
    }

    @Test
    void shouldRejectNonNumericAmount() {
        assertThatThrownBy(() -> Money.of("abc", "PLN"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid amount format");
    }

    @Test
    void shouldRejectNullAmount() {
        assertThatThrownBy(() -> new Money(null, "PLN"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Amount is required");
    }
}

