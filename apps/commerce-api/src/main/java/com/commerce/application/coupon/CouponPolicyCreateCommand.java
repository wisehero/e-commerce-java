package com.commerce.application.coupon;

import java.time.ZonedDateTime;
import java.util.List;

public record CouponPolicyCreateCommand(
    String name,
    String discountType,
    long discountValue,
    Long maxDiscountAmount,
    long minOrderAmount,
    String scopeType,
    Long scopeTargetId,
    List<GradeOverride> gradeOverrides,
    int validDays,
    ZonedDateTime issuableFrom,
    ZonedDateTime issuableUntil,
    long maxIssueCount,
    boolean active
) {

    /** 등급별 할인 규칙 override 한 건. grade는 MemberGrade 이름 문자열. */
    public record GradeOverride(
        String grade,
        String discountType,
        long discountValue,
        Long maxDiscountAmount,
        long minOrderAmount
    ) {
    }
}
