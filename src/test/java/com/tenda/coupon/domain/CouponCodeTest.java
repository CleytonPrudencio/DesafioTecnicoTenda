package com.tenda.coupon.domain;

import com.tenda.coupon.domain.exception.InvalidCouponCodeException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CouponCodeTest {

    @Test
    void cria_codigo_alfanumerico_de_6_caracteres() {
        var code = CouponCode.of("ABC123");
        assertThat(code.value()).isEqualTo("ABC123");
    }

    @ParameterizedTest(name = "entrada {0} é sanitizada para {1}")
    @CsvSource({
        "'ABC-123', 'ABC123'",
        "'A.B C12@3', 'ABC123'",
        "'!!!ABC123!!!', 'ABC123'",
        "'A_B_C_1_2_3', 'ABC123'",
        "'abc123', 'abc123'",
        "'AbC123', 'AbC123'"
    })
    void remove_caracteres_especiais_antes_de_validar(String raw, String expected) {
        assertThat(CouponCode.of(raw).value()).isEqualTo(expected);
    }

    @Test
    void rejeita_codigo_nulo() {
        assertThatThrownBy(() -> CouponCode.of(null))
            .isInstanceOf(InvalidCouponCodeException.class)
            .hasMessageContaining("code is required");
    }

    @ParameterizedTest(name = "entrada {0} resulta em código menor que 6 e é rejeitada")
    @ValueSource(strings = {"", "A", "AB", "ABC12", "  ", "!!!!!"})
    void rejeita_codigo_menor_que_6_apos_sanitizacao(String raw) {
        assertThatThrownBy(() -> CouponCode.of(raw))
            .isInstanceOf(InvalidCouponCodeException.class)
            .hasMessageContaining("must contain exactly 6 alphanumeric characters");
    }

    @ParameterizedTest(name = "entrada {0} resulta em código maior que 6 e é rejeitada")
    @ValueSource(strings = {"ABC1234", "ABCDEFG", "1234567"})
    void rejeita_codigo_maior_que_6_apos_sanitizacao(String raw) {
        assertThatThrownBy(() -> CouponCode.of(raw))
            .isInstanceOf(InvalidCouponCodeException.class);
    }

    @Test
    void fromStored_reidrata_sem_revalidar() {
        var code = CouponCode.fromStored("X1Y2Z3");
        assertThat(code.value()).isEqualTo("X1Y2Z3");
    }

    @Test
    void equals_e_hashcode_por_valor() {
        assertThat(CouponCode.of("ABC123")).isEqualTo(CouponCode.of("ABC123"));
        assertThat(CouponCode.of("ABC123")).hasSameHashCodeAs(CouponCode.of("ABC123"));
        assertThat(CouponCode.of("ABC123")).isNotEqualTo(CouponCode.of("XYZ987"));
        assertThat(CouponCode.of("ABC123")).isNotEqualTo("ABC123");
        assertThat(CouponCode.of("ABC123")).isEqualTo(CouponCode.of("ABC123"));
        CouponCode same = CouponCode.of("ABC123");
        assertThat(same).isEqualTo(same);
    }

    @Test
    void toString_devolve_o_valor() {
        assertThat(CouponCode.of("ABC123")).hasToString("ABC123");
    }
}
