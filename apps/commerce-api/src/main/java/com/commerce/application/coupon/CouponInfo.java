package com.commerce.application.coupon;

import java.time.ZonedDateTime;

import com.commerce.domain.coupon.DiscountRule;
import com.commerce.domain.coupon.IssuedCoupon;

public record CouponInfo(
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

    public static CouponInfo from(IssuedCoupon coupon, ZonedDateTime now) {
        DiscountRule rule = coupon.getDiscountRule();
        return new CouponInfo(
            coupon.getId(),
            coupon.getPolicyId(),
            coupon.getMemberId(),
            rule.type().name(),
            rule.value(),
            rule.maxDiscountAmount() == null ? null : rule.maxDiscountAmount().amount(),
            rule.minOrderAmount().amount(),
            coupon.getStatus().name(),
            coupon.getIssuedAt(),
            coupon.getExpiresAt(),
            coupon.getUsedOrderId(),
            coupon.isExpired(now)
        );
    }
}
