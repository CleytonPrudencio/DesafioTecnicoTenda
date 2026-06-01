package com.tenda.coupon.domain.exception;

public class InvalidCouponCodeException extends DomainException {
    public InvalidCouponCodeException(String message) {
        super(message);
    }
}
