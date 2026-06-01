package com.tenda.coupon.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface CouponJpaRepository extends JpaRepository<CouponEntity, Long> {

    List<CouponEntity> findAllByOrderByIdDesc();
}
