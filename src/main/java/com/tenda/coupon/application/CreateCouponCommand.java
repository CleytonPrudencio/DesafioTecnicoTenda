package com.tenda.coupon.application;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateCouponCommand(
    String code,
    String description,
    BigDecimal discountValue,
    LocalDate expirationDate,
    boolean published
) {}
