package com.tenda.coupon.domain;

import com.tenda.coupon.domain.exception.CouponAlreadyDeletedException;
import com.tenda.coupon.domain.exception.InvalidCouponCodeException;
import com.tenda.coupon.domain.exception.InvalidDescriptionException;
import com.tenda.coupon.domain.exception.InvalidDiscountValueException;
import com.tenda.coupon.domain.exception.InvalidExpirationDateException;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CouponTest {

    private static final Instant NOW = Instant.parse("2026-05-31T12:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final LocalDate FUTURE = LocalDate.of(2099, 12, 31);

    @Test
    void cria_cupom_aplicando_todas_as_regras() {
        Coupon coupon = Coupon.create("ABC-123", "Boas-vindas", new BigDecimal("10.00"), FUTURE, false, FIXED_CLOCK);

        assertThat(coupon.id()).isNull();
        assertThat(coupon.code().value()).isEqualTo("ABC123");
        assertThat(coupon.description().value()).isEqualTo("Boas-vindas");
        assertThat(coupon.discountValue().value()).isEqualByComparingTo("10.00");
        assertThat(coupon.expirationDate().value()).isEqualTo(FUTURE);
        assertThat(coupon.isPublished()).isFalse();
        assertThat(coupon.isDeleted()).isFalse();
        assertThat(coupon.createdAt()).isEqualTo(NOW);
        assertThat(coupon.deletedAt()).isNull();
    }

    @Test
    void cria_cupom_ja_publicado() {
        Coupon coupon = Coupon.create("ABC123", "Desc", new BigDecimal("0.5"), FUTURE, true, FIXED_CLOCK);
        assertThat(coupon.isPublished()).isTrue();
    }

    @Test
    void cria_cupom_com_data_hoje_e_minimo_de_desconto() {
        LocalDate today = LocalDate.now(FIXED_CLOCK);
        Coupon coupon = Coupon.create("ABC123", "Desc", DiscountValue.MINIMUM, today, false, FIXED_CLOCK);
        assertThat(coupon.expirationDate().value()).isEqualTo(today);
        assertThat(coupon.discountValue().value()).isEqualByComparingTo("0.5");
    }

    @Test
    void rejeita_data_no_passado() {
        LocalDate past = LocalDate.now(FIXED_CLOCK).minusDays(1);
        assertThatThrownBy(() -> Coupon.create("ABC123", "Desc", new BigDecimal("1"), past, false, FIXED_CLOCK))
            .isInstanceOf(InvalidExpirationDateException.class);
    }

    @Test
    void propaga_validacoes_dos_value_objects() {
        assertThatThrownBy(() -> Coupon.create("ABC", "Desc", new BigDecimal("1"), FUTURE, false, FIXED_CLOCK))
            .isInstanceOf(InvalidCouponCodeException.class);
        assertThatThrownBy(() -> Coupon.create("ABC123", "", new BigDecimal("1"), FUTURE, false, FIXED_CLOCK))
            .isInstanceOf(InvalidDescriptionException.class);
        assertThatThrownBy(() -> Coupon.create("ABC123", "Desc", new BigDecimal("0.1"), FUTURE, false, FIXED_CLOCK))
            .isInstanceOf(InvalidDiscountValueException.class);
    }

    @Test
    void delete_aplica_soft_delete() {
        Coupon coupon = restored(false);
        Coupon deleted = coupon.delete(FIXED_CLOCK);

        assertThat(deleted.isDeleted()).isTrue();
        assertThat(deleted.deletedAt()).isEqualTo(NOW);
        assertThat(deleted.id()).isEqualTo(coupon.id());
        assertThat(deleted.code()).isEqualTo(coupon.code());
        assertThat(coupon.isDeleted()).isFalse();
        assertThat(coupon.deletedAt()).isNull();
    }

    @Test
    void delete_em_cupom_ja_deletado_lanca_excecao() {
        Coupon coupon = restored(true);
        assertThatThrownBy(() -> coupon.delete(FIXED_CLOCK))
            .isInstanceOf(CouponAlreadyDeletedException.class)
            .hasMessageContaining("already deleted");
    }

    @Test
    void withId_atribui_id_em_um_cupom_novo() {
        Coupon coupon = Coupon.create("ABC123", "Desc", new BigDecimal("1"), FUTURE, false, FIXED_CLOCK);
        Coupon withId = coupon.withId(42L);
        assertThat(withId.id()).isEqualTo(42L);
        assertThat(coupon.id()).isNull();
    }

    @Test
    void restore_reidrata_sem_revalidar() {
        LocalDate past = LocalDate.of(2000, 1, 1);
        Coupon coupon = Coupon.restore(
            1L, "ABC123", "Desc", new BigDecimal("0.10"),
            past, true, true, NOW, NOW
        );
        assertThat(coupon.id()).isEqualTo(1L);
        assertThat(coupon.expirationDate().value()).isEqualTo(past);
        assertThat(coupon.isDeleted()).isTrue();
    }

    @Test
    void equals_e_hashcode_consideram_todos_os_campos() {
        Coupon a = restored(false);
        Coupon b = restored(false);
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a).isEqualTo(a);
        assertThat(a).isNotEqualTo(null);
        assertThat(a).isNotEqualTo("string");
        assertThat(a).isNotEqualTo(a.delete(FIXED_CLOCK));
    }

    private static Coupon restored(boolean deleted) {
        return Coupon.restore(
            1L, "ABC123", "Desc", new BigDecimal("1.00"),
            FUTURE, false, deleted, NOW, deleted ? NOW : null
        );
    }
}
