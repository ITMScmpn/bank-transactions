package com.banktransactions.features.importing.domain.valueobject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

class IbanTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "PL61109010140000071219812874",
            "DE89370400440532013000",
            "GB29NWBK60161331926819"
    })
    void shouldAcceptValidIban(String raw) {
        assertThatNoException().isThrownBy(() -> new Iban(raw));
    }

    @Test
    void shouldNormalizeToUpperCase() {
        Iban iban = new Iban("pl61109010140000071219812874");
        assertThat(iban.getValue()).isEqualTo("PL61109010140000071219812874");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "invalid-iban", "1234567890", "pl 61 1090"})
    void shouldRejectInvalidIban(String raw) {
        assertThatThrownBy(() -> new Iban(raw)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectNull() {
        assertThatThrownBy(() -> new Iban(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("IBAN is required");
    }

    @Test
    void shouldImplementEquality() {
        Iban a = new Iban("PL61109010140000071219812874");
        Iban b = new Iban("PL61109010140000071219812874");
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }
}

