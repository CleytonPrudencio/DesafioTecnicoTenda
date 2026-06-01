package com.tenda.coupon.application;

import com.tenda.coupon.application.port.CouponRepository;
import com.tenda.coupon.domain.Coupon;
import com.tenda.coupon.domain.exception.CouponAlreadyDeletedException;
import com.tenda.coupon.domain.exception.CouponNotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    private static final Instant NOW = Instant.parse("2026-05-31T12:00:00Z");
    private static final LocalDate FUTURE = LocalDate.of(2099, 12, 31);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    @Mock
    CouponRepository repository;

    CouponService service;

    @BeforeEach
    void setUp() {
        service = new CouponService(repository, clock);
    }

    @Test
    void create_chama_repository_com_o_dominio_validado() {
        when(repository.save(ArgumentMatchers.any(Coupon.class)))
            .thenAnswer(inv -> ((Coupon) inv.getArgument(0)).withId(99L));

        Coupon created = service.create(new CreateCouponCommand(
            "ABC-123", "Desc", new BigDecimal("5.00"), FUTURE, true
        ));

        ArgumentCaptor<Coupon> captor = ArgumentCaptor.forClass(Coupon.class);
        verify(repository).save(captor.capture());
        Coupon sent = captor.getValue();
        assertThat(sent.code().value()).isEqualTo("ABC123");
        assertThat(sent.isPublished()).isTrue();
        assertThat(sent.isDeleted()).isFalse();
        assertThat(created.id()).isEqualTo(99L);
    }

    @Test
    void delete_busca_aplica_soft_delete_e_persiste() {
        Coupon existing = Coupon.restore(
            5L, "ABC123", "Desc", new BigDecimal("1.00"),
            FUTURE, false, false, NOW, null
        );
        when(repository.findById(5L)).thenReturn(Optional.of(existing));
        when(repository.save(ArgumentMatchers.any(Coupon.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        Coupon result = service.delete(5L);

        assertThat(result.isDeleted()).isTrue();
        assertThat(result.deletedAt()).isEqualTo(NOW);
        verify(repository).save(ArgumentMatchers.argThat(c -> c.isDeleted() && c.deletedAt() != null));
    }

    @Test
    void delete_lanca_quando_nao_encontrado() {
        when(repository.findById(404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.delete(404L))
            .isInstanceOf(CouponNotFoundException.class);
        verify(repository, never()).save(ArgumentMatchers.any());
    }

    @Test
    void delete_lanca_quando_ja_deletado() {
        Coupon deleted = Coupon.restore(
            6L, "ABC123", "Desc", new BigDecimal("1.00"),
            FUTURE, false, true, NOW, NOW
        );
        when(repository.findById(6L)).thenReturn(Optional.of(deleted));

        assertThatThrownBy(() -> service.delete(6L))
            .isInstanceOf(CouponAlreadyDeletedException.class);
        verify(repository, never()).save(ArgumentMatchers.any());
    }

    @Test
    void getById_devolve_cupom_quando_encontrado() {
        Coupon existing = Coupon.restore(
            7L, "ABC123", "Desc", new BigDecimal("1.00"),
            FUTURE, false, false, NOW, null
        );
        when(repository.findById(7L)).thenReturn(Optional.of(existing));

        assertThat(service.getById(7L)).isSameAs(existing);
    }

    @Test
    void getById_lanca_quando_nao_encontrado() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getById(99L))
            .isInstanceOf(CouponNotFoundException.class);
    }

    @Test
    void list_delega_para_o_repositorio() {
        Coupon c1 = Coupon.restore(1L, "ABC123", "x", new BigDecimal("1"), FUTURE, false, false, NOW, null);
        Coupon c2 = Coupon.restore(2L, "DEF456", "x", new BigDecimal("1"), FUTURE, false, false, NOW, null);
        when(repository.findAll()).thenReturn(List.of(c2, c1));

        assertThat(service.list()).containsExactly(c2, c1);
    }
}
