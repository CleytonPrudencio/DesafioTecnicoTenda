package com.tenda.coupon.domain;

import com.tenda.coupon.domain.exception.CouponAlreadyDeletedException;
import com.tenda.coupon.domain.exception.CouponNotFoundException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExceptionsTest {

    @Test
    void couponNotFound_inclui_id() {
        assertThat(new CouponNotFoundException(42L).getMessage()).contains("42");
    }

    @Test
    void couponAlreadyDeleted_inclui_id() {
        assertThat(new CouponAlreadyDeletedException(7L).getMessage()).contains("7");
    }
}
