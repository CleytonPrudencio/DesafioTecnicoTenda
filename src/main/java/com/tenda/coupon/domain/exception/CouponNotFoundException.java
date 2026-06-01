package com.tenda.coupon.domain.exception;

public class CouponNotFoundException extends DomainException {
    public CouponNotFoundException(Long id) {
        super("Coupon not found: id=" + id);
    }
}
