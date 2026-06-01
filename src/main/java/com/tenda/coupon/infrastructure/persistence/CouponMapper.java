package com.tenda.coupon.infrastructure.persistence;

import com.tenda.coupon.domain.Coupon;

final class CouponMapper {

    private CouponMapper() {}

    static CouponEntity toEntity(Coupon coupon) {
        return new CouponEntity(
            coupon.id(),
            coupon.code().value(),
            coupon.description().value(),
            coupon.discountValue().value(),
            coupon.expirationDate().value(),
            coupon.isPublished(),
            coupon.isDeleted(),
            coupon.createdAt(),
            coupon.deletedAt()
        );
    }

    static Coupon toDomain(CouponEntity entity) {
        return Coupon.restore(
            entity.getId(),
            entity.getCode(),
            entity.getDescription(),
            entity.getDiscountValue(),
            entity.getExpirationDate(),
            entity.isPublished(),
            entity.isDeleted(),
            entity.getCreatedAt(),
            entity.getDeletedAt()
        );
    }
}
