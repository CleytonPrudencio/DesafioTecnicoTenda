package com.tenda.coupon.domain;

import com.tenda.coupon.domain.exception.InvalidDescriptionException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DescriptionTest {

    @Test
    void aceita_descricao_valida() {
        assertThat(Description.of("Cupom de boas-vindas").value()).isEqualTo("Cupom de boas-vindas");
    }

    @Test
    void faz_trim_da_descricao() {
        assertThat(Description.of("   texto   ").value()).isEqualTo("texto");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   ", "\t"})
    void rejeita_descricao_em_branco(String value) {
        assertThatThrownBy(() -> Description.of(value))
            .isInstanceOf(InvalidDescriptionException.class)
            .hasMessageContaining("description is required");
    }

    @Test
    void fromStored_nao_revalida() {
        assertThat(Description.fromStored("").value()).isEmpty();
    }

    @Test
    void equals_e_hashcode() {
        Description a = Description.of("X");
        Description b = Description.of("X");
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(Description.of("Y"));
        assertThat(a).isNotEqualTo("X");
        assertThat(a).isEqualTo(a);
        assertThat(a.toString()).isEqualTo("X");
    }
}
