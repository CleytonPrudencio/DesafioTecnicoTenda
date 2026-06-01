package com.tenda.coupon.domain;

import com.tenda.coupon.domain.exception.InvalidDiscountValueException;

import java.math.BigDecimal;
import java.util.Objects;

public final class DiscountValue {

    public static final BigDecimal MINIMUM = new BigDecimal("0.5");

    private final BigDecimal value;

    private DiscountValue(BigDecimal value) {
        this.value = value;
    }

    public static DiscountValue of(BigDecimal raw) {
        if (raw == null) {
            throw new InvalidDiscountValueException("discountValue is required");
        }
        if (raw.compareTo(MINIMUM) < 0) {
            throw new InvalidDiscountValueException(
                "discountValue must be greater than or equal to " + MINIMUM + " (got " + raw.toPlainString() + ")"
            );
        }
        return new DiscountValue(raw);
    }

    public static DiscountValue fromStored(BigDecimal storedValue) {
        Objects.requireNonNull(storedValue, "storedValue");
        return new DiscountValue(storedValue);
    }

    public BigDecimal value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DiscountValue that)) return false;
        return value.compareTo(that.value) == 0;
    }

    @Override
    public int hashCode() {
        return value.stripTrailingZeros().hashCode();
    }

    @Override
    public String toString() {
        return value.toPlainString();
    }
}
