package com.commerce.application.coupon;

import java.time.ZonedDateTime;

public record CouponPolicyCreateCommand(
    String name,
    String discountType,
    long discountValue,
    Long maxDiscountAmount,
    long minOrderAmount,
    int validDays,
    ZonedDateTime issuableFrom,
    ZonedDateTime issuableUntil,
    long maxIssueCount,
    boolean active
) {
}
