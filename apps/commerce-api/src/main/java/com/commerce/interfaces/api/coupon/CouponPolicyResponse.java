package com.commerce.interfaces.api.coupon;

import java.time.ZonedDateTime;

import com.commerce.application.coupon.CouponPolicyInfo;

public record CouponPolicyResponse(
    Long id,
    String name,
    String discountType,
    long discountValue,
    Long maxDiscountAmount,
    long minOrderAmount,
    int validDays,
    ZonedDateTime issuableFrom,
    ZonedDateTime issuableUntil,
    long maxIssueCount,
    long issuedCount,
    boolean active
) {

    public static CouponPolicyResponse from(CouponPolicyInfo info) {
        return new CouponPolicyResponse(
            info.id(),
            info.name(),
            info.discountType(),
            info.discountValue(),
            info.maxDiscountAmount(),
            info.minOrderAmount(),
            info.validDays(),
            info.issuableFrom(),
            info.issuableUntil(),
            info.maxIssueCount(),
            info.issuedCount(),
            info.active()
        );
    }
}
