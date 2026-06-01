package com.tenda.coupon.domain;

import com.tenda.coupon.domain.exception.CouponAlreadyDeletedException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public final class Coupon {

    private final Long id;
    private final CouponCode code;
    private final Description description;
    private final DiscountValue discountValue;
    private final ExpirationDate expirationDate;
    private final boolean published;
    private final boolean deleted;
    private final Instant createdAt;
    private final Instant deletedAt;

    private Coupon(Long id, CouponCode code, Description description, DiscountValue discountValue,
                   ExpirationDate expirationDate, boolean published, boolean deleted,
                   Instant createdAt, Instant deletedAt) {
        this.id = id;
        this.code = Objects.requireNonNull(code, "code");
        this.description = Objects.requireNonNull(description, "description");
        this.discountValue = Objects.requireNonNull(discountValue, "discountValue");
        this.expirationDate = Objects.requireNonNull(expirationDate, "expirationDate");
        this.published = published;
        this.deleted = deleted;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.deletedAt = deletedAt;
    }

    public static Coupon create(
        String rawCode,
        String description,
        BigDecimal discountValue,
        LocalDate expirationDate,
        boolean published,
        Clock clock
    ) {
        Objects.requireNonNull(clock, "clock");
        return new Coupon(
            null,
            CouponCode.of(rawCode),
            Description.of(description),
            DiscountValue.of(discountValue),
            ExpirationDate.of(expirationDate, LocalDate.now(clock)),
            published,
            false,
            Instant.now(clock),
            null
        );
    }

    public static Coupon restore(
        Long id,
        String code,
        String description,
        BigDecimal discountValue,
        LocalDate expirationDate,
        boolean published,
        boolean deleted,
        Instant createdAt,
        Instant deletedAt
    ) {
        Objects.requireNonNull(id, "id");
        return new Coupon(
            id,
            CouponCode.fromStored(code),
            Description.fromStored(description),
            DiscountValue.fromStored(discountValue),
            ExpirationDate.fromStored(expirationDate),
            published,
            deleted,
            createdAt,
            deletedAt
        );
    }

    public Coupon delete(Clock clock) {
        Objects.requireNonNull(clock, "clock");
        if (deleted) {
            throw new CouponAlreadyDeletedException(id);
        }
        return new Coupon(
            id, code, description, discountValue, expirationDate,
            published, true, createdAt, Instant.now(clock)
        );
    }

    public Coupon withId(Long newId) {
        Objects.requireNonNull(newId, "newId");
        return new Coupon(
            newId, code, description, discountValue, expirationDate,
            published, deleted, createdAt, deletedAt
        );
    }

    public Long id() { return id; }
    public CouponCode code() { return code; }
    public Description description() { return description; }
    public DiscountValue discountValue() { return discountValue; }
    public ExpirationDate expirationDate() { return expirationDate; }
    public boolean isPublished() { return published; }
    public boolean isDeleted() { return deleted; }
    public Instant createdAt() { return createdAt; }
    public Instant deletedAt() { return deletedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Coupon other)) return false;
        return Objects.equals(id, other.id)
            && code.equals(other.code)
            && description.equals(other.description)
            && discountValue.equals(other.discountValue)
            && expirationDate.equals(other.expirationDate)
            && published == other.published
            && deleted == other.deleted
            && Objects.equals(createdAt, other.createdAt)
            && Objects.equals(deletedAt, other.deletedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, code, description, discountValue, expirationDate, published, deleted, createdAt, deletedAt);
    }
}
