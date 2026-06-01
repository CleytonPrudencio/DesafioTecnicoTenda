package com.tenda.coupon.domain.exception;

public class InvalidExpirationDateException extends DomainException {
    public InvalidExpirationDateException(String message) {
        super(message);
    }
}
