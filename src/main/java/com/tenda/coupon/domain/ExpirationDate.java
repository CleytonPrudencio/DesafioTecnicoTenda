package com.tenda.coupon.domain;

import com.tenda.coupon.domain.exception.InvalidExpirationDateException;

import java.time.LocalDate;
import java.util.Objects;

public final class ExpirationDate {

    private final LocalDate value;

    private ExpirationDate(LocalDate value) {
        this.value = value;
    }

    public static ExpirationDate of(LocalDate raw, LocalDate today) {
        if (raw == null) {
            throw new InvalidExpirationDateException("expirationDate is required");
        }
        Objects.requireNonNull(today, "today");
        if (raw.isBefore(today)) {
            throw new InvalidExpirationDateException(
                "expirationDate cannot be in the past (got " + raw + ", today is " + today + ")"
            );
        }
        return new ExpirationDate(raw);
    }

    public static ExpirationDate fromStored(LocalDate storedValue) {
        Objects.requireNonNull(storedValue, "storedValue");
        return new ExpirationDate(storedValue);
    }

    public LocalDate value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExpirationDate that)) return false;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
