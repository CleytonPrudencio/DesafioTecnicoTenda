package com.tenda.coupon.domain;

import com.tenda.coupon.domain.exception.InvalidCouponCodeException;

import java.util.Objects;
import java.util.regex.Pattern;

public final class CouponCode {

    public static final int REQUIRED_LENGTH = 6;
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^A-Za-z0-9]");

    private final String value;

    private CouponCode(String value) {
        this.value = value;
    }

    public static CouponCode of(String raw) {
        if (raw == null) {
            throw new InvalidCouponCodeException("code is required");
        }
        String sanitized = NON_ALPHANUMERIC.matcher(raw).replaceAll("");
        if (sanitized.length() != REQUIRED_LENGTH) {
            throw new InvalidCouponCodeException(
                "code must contain exactly " + REQUIRED_LENGTH + " alphanumeric characters after removing special characters (got '"
                    + sanitized + "' with length " + sanitized.length() + ")"
            );
        }
        return new CouponCode(sanitized);
    }

    public static CouponCode fromStored(String storedValue) {
        Objects.requireNonNull(storedValue, "storedValue");
        return new CouponCode(storedValue);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CouponCode that)) return false;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
