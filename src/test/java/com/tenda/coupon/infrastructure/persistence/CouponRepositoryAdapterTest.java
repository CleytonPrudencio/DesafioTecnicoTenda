package com.tenda.coupon.infrastructure.persistence;

import com.tenda.coupon.domain.Coupon;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(CouponRepositoryAdapter.class)
class CouponRepositoryAdapterTest {

    private static final Instant NOW = Instant.parse("2026-05-31T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final LocalDate FUTURE = LocalDate.of(2099, 12, 31);

    @Autowired
    CouponRepositoryAdapter adapter;

    @Test
    void persiste_e_recupera_cupom_preservando_atributos_de_dominio() {
        Coupon novo = Coupon.create("AB!C-123", "Desc completa", new BigDecimal("12.50"), FUTURE, true, CLOCK);

        Coupon salvo = adapter.save(novo);

        assertThat(salvo.id()).isNotNull();
        assertThat(salvo.code().value()).isEqualTo("ABC123");
        assertThat(salvo.description().value()).isEqualTo("Desc completa");
        assertThat(salvo.discountValue().value()).isEqualByComparingTo("12.50");
        assertThat(salvo.expirationDate().value()).isEqualTo(FUTURE);
        assertThat(salvo.isPublished()).isTrue();
        assertThat(salvo.isDeleted()).isFalse();
        assertThat(salvo.createdAt()).isEqualTo(NOW);
        assertThat(salvo.deletedAt()).isNull();

        Optional<Coupon> achado = adapter.findById(salvo.id());
        assertThat(achado).isPresent();
        assertThat(achado.get().code().value()).isEqualTo("ABC123");
    }

    @Test
    void findById_retorna_vazio_quando_nao_existe() {
        assertThat(adapter.findById(999_999L)).isEmpty();
    }

    @Test
    void soft_delete_persiste_flag_e_data() {
        Coupon novo = Coupon.create("ZZZ999", "X", new BigDecimal("1"), FUTURE, false, CLOCK);
        Coupon salvo = adapter.save(novo);

        Coupon deletado = adapter.save(salvo.delete(CLOCK));

        Optional<Coupon> reachado = adapter.findById(deletado.id());
        assertThat(reachado).isPresent();
        assertThat(reachado.get().isDeleted()).isTrue();
        assertThat(reachado.get().deletedAt()).isEqualTo(NOW);
    }

    @Test
    void findAll_devolve_em_ordem_decrescente_de_id() {
        Coupon c1 = adapter.save(Coupon.create("AAA111", "x", new BigDecimal("1"), FUTURE, false, CLOCK));
        Coupon c2 = adapter.save(Coupon.create("BBB222", "x", new BigDecimal("1"), FUTURE, false, CLOCK));
        Coupon c3 = adapter.save(Coupon.create("CCC333", "x", new BigDecimal("1"), FUTURE, false, CLOCK));

        List<Coupon> all = adapter.findAll();

        assertThat(all).extracting(Coupon::id).containsExactly(c3.id(), c2.id(), c1.id());
    }
}
