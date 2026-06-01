package com.tenda.coupon.infrastructure.persistence;

import com.tenda.coupon.application.port.CouponRepository;
import com.tenda.coupon.domain.Coupon;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
class CouponRepositoryAdapter implements CouponRepository {

    private final CouponJpaRepository jpa;

    CouponRepositoryAdapter(CouponJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Coupon save(Coupon coupon) {
        CouponEntity saved = jpa.save(CouponMapper.toEntity(coupon));
        return CouponMapper.toDomain(saved);
    }

    @Override
    public Optional<Coupon> findById(Long id) {
        return jpa.findById(id).map(CouponMapper::toDomain);
    }

    @Override
    public List<Coupon> findAll() {
        return jpa.findAllByOrderByIdDesc().stream()
            .map(CouponMapper::toDomain)
            .toList();
    }
}
