package com.commerce.application.coupon;

import java.time.ZonedDateTime;
import java.util.List;

import com.commerce.domain.coupon.DiscountType;
import com.commerce.domain.coupon.ScopeType;
import com.commerce.domain.member.MemberGrade;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

/**
 * 쿠폰 정책 생성 Use Case의 입력 경계 객체.
 *
 * <p>HTTP 경계에서 넘어온 raw string(할인 유형·적용 범위·회원 등급)을 도메인 enum으로 해석하는
 * 책임을 {@link #of} 정적 팩토리에 둔다. interfaces 계층이 domain enum을 직접 참조하지 않도록
 * 변환을 application 경계 객체로 격리하고, UseCase는 이미 도메인 언어인 enum만 다룬다.
 */
public record CouponPolicyCreateCommand(
    String name,
    DiscountType discountType,
    long discountValue,
    Long maxDiscountAmount,
    long minOrderAmount,
    ScopeType scopeType,
    Long scopeTargetId,
    List<GradeOverride> gradeOverrides,
    int validDays,
    ZonedDateTime issuableFrom,
    ZonedDateTime issuableUntil,
    long maxIssueCount,
    boolean active
) {

    /** raw string으로 들어온 할인 유형·적용 범위를 도메인 enum으로 해석해 Command를 만든다. */
    public static CouponPolicyCreateCommand of(
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
        return new CouponPolicyCreateCommand(
            name,
            parseDiscountType(discountType),
            discountValue,
            maxDiscountAmount,
            minOrderAmount,
            parseScopeType(scopeType),
            scopeTargetId,
            gradeOverrides,
            validDays,
            issuableFrom,
            issuableUntil,
            maxIssueCount,
            active
        );
    }

    private static DiscountType parseDiscountType(String value) {
        try {
            return DiscountType.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new CoreException(ErrorType.BAD_REQUEST, "지원하지 않는 할인 유형입니다: " + value);
        }
    }

    /** 적용 범위는 선택값이다. null·빈 문자열이면 주문 전체(WHOLE)로 본다. */
    private static ScopeType parseScopeType(String value) {
        if (value == null || value.isBlank()) {
            return ScopeType.WHOLE;
        }
        try {
            return ScopeType.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new CoreException(ErrorType.BAD_REQUEST, "지원하지 않는 적용 범위입니다: " + value);
        }
    }

    /** 등급별 할인 규칙 override 한 건. 도메인 enum으로 해석된 상태를 보유한다. */
    public record GradeOverride(
        MemberGrade grade,
        DiscountType discountType,
        long discountValue,
        Long maxDiscountAmount,
        long minOrderAmount
    ) {

        /** raw string으로 들어온 회원 등급·할인 유형을 도메인 enum으로 해석해 만든다. */
        public static GradeOverride of(
            String grade,
            String discountType,
            long discountValue,
            Long maxDiscountAmount,
            long minOrderAmount
        ) {
            return new GradeOverride(
                parseGrade(grade),
                parseDiscountType(discountType),
                discountValue,
                maxDiscountAmount,
                minOrderAmount
            );
        }

        private static MemberGrade parseGrade(String value) {
            try {
                return MemberGrade.valueOf(value);
            } catch (IllegalArgumentException | NullPointerException e) {
                throw new CoreException(ErrorType.BAD_REQUEST, "지원하지 않는 회원 등급입니다: " + value);
            }
        }
    }
}
