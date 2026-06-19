package com.commerce.domain.coupon;

import java.time.ZonedDateTime;
import java.util.Map;

import com.commerce.domain.member.MemberGrade;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

import lombok.Getter;

@Getter
public class CouponPolicy {

    private Long id;
    private String name;
    private DiscountRule discountRule;
    private ApplicabilityScope applicabilityScope;
    private Map<MemberGrade, DiscountRule> gradeOverrides;
    private int validDays;
    private ZonedDateTime issuableFrom;
    private ZonedDateTime issuableUntil;
    private long maxIssueCount;
    private long issuedCount;
    private boolean active;

    private CouponPolicy(Long id, String name, DiscountRule discountRule, ApplicabilityScope applicabilityScope,
        Map<MemberGrade, DiscountRule> gradeOverrides, int validDays, ZonedDateTime issuableFrom,
        ZonedDateTime issuableUntil, long maxIssueCount, long issuedCount, boolean active) {
        this.id = id;
        this.name = name;
        this.discountRule = discountRule;
        this.applicabilityScope = applicabilityScope;
        this.gradeOverrides = gradeOverrides == null ? Map.of() : Map.copyOf(gradeOverrides);
        this.validDays = validDays;
        this.issuableFrom = issuableFrom;
        this.issuableUntil = issuableUntil;
        this.maxIssueCount = maxIssueCount;
        this.issuedCount = issuedCount;
        this.active = active;
        validate();
    }

    public static CouponPolicy create(String name, DiscountRule discountRule, ApplicabilityScope applicabilityScope,
        Map<MemberGrade, DiscountRule> gradeOverrides, int validDays, ZonedDateTime issuableFrom,
        ZonedDateTime issuableUntil, long maxIssueCount, boolean active) {
        return new CouponPolicy(null, name, discountRule, applicabilityScope, gradeOverrides, validDays,
            issuableFrom, issuableUntil, maxIssueCount, 0, active);
    }

    public static CouponPolicy reconstitute(Long id, String name, DiscountRule discountRule,
        ApplicabilityScope applicabilityScope, Map<MemberGrade, DiscountRule> gradeOverrides, int validDays,
        ZonedDateTime issuableFrom, ZonedDateTime issuableUntil, long maxIssueCount, long issuedCount,
        boolean active) {
        return new CouponPolicy(id, name, discountRule, applicabilityScope, gradeOverrides, validDays,
            issuableFrom, issuableUntil, maxIssueCount, issuedCount, active);
    }

    /**
     * 발급 시 회원 등급으로 적용할 단일 할인 규칙을 해소한다.
     * 등급 override가 있으면 그 규칙을, 없으면 기본 규칙을 쓴다.
     */
    public DiscountRule resolveRuleFor(MemberGrade grade) {
        return gradeOverrides.getOrDefault(grade, discountRule);
    }

    public void assertIssuable(ZonedDateTime now) {
        if (!active) {
            throw new CoreException(ErrorType.BAD_REQUEST, "발급 중단된 쿠폰 정책입니다.");
        }
        if (now == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "발급 기준 시각은 필수입니다.");
        }
        if (now.isBefore(issuableFrom) || !now.isBefore(issuableUntil)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰 발급 가능 기간이 아닙니다.");
        }
        if (issuedCount >= maxIssueCount) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰이 모두 소진되었습니다.");
        }
    }

    private void validate() {
        if (name == null || name.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰 정책명은 필수입니다.");
        }
        if (discountRule == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "할인 규칙은 필수입니다.");
        }
        if (applicabilityScope == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "적용 범위는 필수입니다.");
        }
        if (validDays <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰 유효일수는 1일 이상이어야 합니다.");
        }
        if (issuableFrom == null || issuableUntil == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "발급 가능 기간은 필수입니다.");
        }
        if (!issuableFrom.isBefore(issuableUntil)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "발급 시작 시각은 종료 시각보다 빨라야 합니다.");
        }
        if (maxIssueCount <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "최대 발급 수량은 1개 이상이어야 합니다.");
        }
        if (issuedCount < 0 || issuedCount > maxIssueCount) {
            throw new CoreException(ErrorType.BAD_REQUEST, "발급 수량이 올바르지 않습니다.");
        }
    }
}
