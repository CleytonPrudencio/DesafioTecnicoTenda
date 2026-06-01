package com.tenda.coupon.domain;

import com.tenda.coupon.domain.exception.InvalidDiscountValueException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DiscountValueTest {

    @Test
    void aceita_valor_minimo() {
        assertThat(DiscountValue.of(new BigDecimal("0.5")).value()).isEqualByComparingTo("0.5");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.50", "0.500", "1", "10", "9999999.99"})
    void aceita_qualquer_valor_acima_do_minimo(String value) {
        assertThat(DiscountValue.of(new BigDecimal(value)).value()).isEqualByComparingTo(value);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.49", "0.4999", "0", "-1", "-0.5"})
    void rejeita_valor_abaixo_do_minimo(String value) {
        assertThatThrownBy(() -> DiscountValue.of(new BigDecimal(value)))
            .isInstanceOf(InvalidDiscountValueException.class)
            .hasMessageContaining("must be greater than or equal to 0.5");
    }

    @Test
    void rejeita_valor_nulo() {
        assertThatThrownBy(() -> DiscountValue.of(null))
            .isInstanceOf(InvalidDiscountValueException.class)
            .hasMessageContaining("discountValue is required");
    }

    @Test
    void fromStored_nao_revalida() {
        assertThat(DiscountValue.fromStored(new BigDecimal("0.10")).value())
            .isEqualByComparingTo("0.10");
    }

    @Test
    void equals_e_hashcode_por_valor() {
        DiscountValue a = DiscountValue.of(new BigDecimal("1.50"));
        DiscountValue b = DiscountValue.of(new BigDecimal("1.50"));
        DiscountValue c = DiscountValue.of(new BigDecimal("2.00"));
        assertThat(a).isEqualTo(b);
        assertThat(a).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(c);
        assertThat(a).isNotEqualTo("1.50");
        assertThat(a).isEqualTo(a);
        assertThat(a.toString()).isEqualTo("1.50");
    }
}
