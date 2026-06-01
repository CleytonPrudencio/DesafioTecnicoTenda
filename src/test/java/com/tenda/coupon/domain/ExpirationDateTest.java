package com.tenda.coupon.domain;

import com.tenda.coupon.domain.exception.InvalidExpirationDateException;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExpirationDateTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 5, 31);

    @Test
    void aceita_data_hoje() {
        assertThat(ExpirationDate.of(TODAY, TODAY).value()).isEqualTo(TODAY);
    }

    @Test
    void aceita_data_futura() {
        LocalDate future = TODAY.plusDays(10);
        assertThat(ExpirationDate.of(future, TODAY).value()).isEqualTo(future);
    }

    @Test
    void rejeita_data_passada() {
        LocalDate past = TODAY.minusDays(1);
        assertThatThrownBy(() -> ExpirationDate.of(past, TODAY))
            .isInstanceOf(InvalidExpirationDateException.class)
            .hasMessageContaining("cannot be in the past");
    }

    @Test
    void rejeita_data_nula() {
        assertThatThrownBy(() -> ExpirationDate.of(null, TODAY))
            .isInstanceOf(InvalidExpirationDateException.class)
            .hasMessageContaining("expirationDate is required");
    }

    @Test
    void fromStored_nao_revalida() {
        LocalDate past = LocalDate.of(2000, 1, 1);
        assertThat(ExpirationDate.fromStored(past).value()).isEqualTo(past);
    }

    @Test
    void equals_e_hashcode() {
        ExpirationDate a = ExpirationDate.of(TODAY, TODAY);
        ExpirationDate b = ExpirationDate.of(TODAY, TODAY);
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(ExpirationDate.of(TODAY.plusDays(1), TODAY));
        assertThat(a).isNotEqualTo("2026-05-31");
        assertThat(a).isEqualTo(a);
        assertThat(a.toString()).isEqualTo(TODAY.toString());
    }
}
