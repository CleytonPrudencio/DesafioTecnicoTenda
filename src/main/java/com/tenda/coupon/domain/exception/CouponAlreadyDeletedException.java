package com.tenda.coupon.domain.exception;

public class CouponAlreadyDeletedException extends DomainException {
    public CouponAlreadyDeletedException(Long id) {
        super("Coupon already deleted: id=" + id);
    }
}
