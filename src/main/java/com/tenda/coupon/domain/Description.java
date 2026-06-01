package com.tenda.coupon.domain;

import com.tenda.coupon.domain.exception.InvalidDescriptionException;

import java.util.Objects;

public final class Description {

    private final String value;

    private Description(String value) {
        this.value = value;
    }

    public static Description of(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidDescriptionException("description is required");
        }
        return new Description(raw.trim());
    }

    public static Description fromStored(String storedValue) {
        Objects.requireNonNull(storedValue, "storedValue");
        return new Description(storedValue);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Description that)) return false;
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
