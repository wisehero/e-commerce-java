package com.commerce.interfaces.api.coupon;

import java.time.ZonedDateTime;

import com.commerce.application.coupon.CouponInfo;

public record CouponResponse(
    Long id,
    Long policyId,
    Long memberId,
    String discountType,
    long discountValue,
    Long maxDiscountAmount,
    long minOrderAmount,
    String status,
    ZonedDateTime issuedAt,
    ZonedDateTime expiresAt,
    Long usedOrderId,
    boolean expired
) {

    public static CouponResponse from(CouponInfo info) {
        return new CouponResponse(
            info.id(),
            info.policyId(),
            info.memberId(),
            info.discountType(),
            info.discountValue(),
            info.maxDiscountAmount(),
            info.minOrderAmount(),
            info.status(),
            info.issuedAt(),
            info.expiresAt(),
            info.usedOrderId(),
            info.expired()
        );
    }
}
