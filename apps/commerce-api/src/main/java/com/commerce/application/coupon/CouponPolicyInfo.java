package com.commerce.application.coupon;

import java.time.ZonedDateTime;

import com.commerce.domain.coupon.CouponPolicy;
import com.commerce.domain.coupon.DiscountRule;

public record CouponPolicyInfo(
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

    public static CouponPolicyInfo from(CouponPolicy policy) {
        DiscountRule rule = policy.getDiscountRule();
        return new CouponPolicyInfo(
            policy.getId(),
            policy.getName(),
            rule.type().name(),
            rule.value(),
            rule.maxDiscountAmount() == null ? null : rule.maxDiscountAmount().amount(),
            rule.minOrderAmount().amount(),
            policy.getValidDays(),
            policy.getIssuableFrom(),
            policy.getIssuableUntil(),
            policy.getMaxIssueCount(),
            policy.getIssuedCount(),
            policy.isActive()
        );
    }
}
