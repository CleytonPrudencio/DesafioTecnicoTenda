package com.tenda.coupon.application;

import com.tenda.coupon.application.port.CouponRepository;
import com.tenda.coupon.domain.Coupon;
import com.tenda.coupon.domain.exception.CouponNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

@Service
public class CouponService {

    private final CouponRepository repository;
    private final Clock clock;

    public CouponService(CouponRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public Coupon create(CreateCouponCommand command) {
        Coupon coupon = Coupon.create(
            command.code(),
            command.description(),
            command.discountValue(),
            command.expirationDate(),
            command.published(),
            clock
        );
        return repository.save(coupon);
    }

    @Transactional
    public Coupon delete(Long id) {
        Coupon coupon = repository.findById(id)
            .orElseThrow(() -> new CouponNotFoundException(id));
        return repository.save(coupon.delete(clock));
    }

    @Transactional(readOnly = true)
    public Coupon getById(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new CouponNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Coupon> list() {
        return repository.findAll();
    }
}
