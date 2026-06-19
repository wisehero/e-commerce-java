package com.commerce.domain.coupon;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import com.commerce.domain.member.MemberGrade;
import com.commerce.domain.shared.Money;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

import lombok.Getter;

@Getter
public class IssuedCoupon {

    private Long id;
    private Long policyId;
    private Long memberId;
    private ApplicabilityScope applicabilityScope;
    private DiscountRule discountRule;
    private CouponStatus status;
    private ZonedDateTime issuedAt;
    private ZonedDateTime expiresAt;
    private Long usedOrderId;

    private IssuedCoupon(Long id, Long policyId, Long memberId, ApplicabilityScope applicabilityScope,
        DiscountRule discountRule, CouponStatus status, ZonedDateTime issuedAt, ZonedDateTime expiresAt,
        Long usedOrderId) {
        this.id = id;
        this.policyId = policyId;
        this.memberId = memberId;
        this.applicabilityScope = applicabilityScope;
        this.discountRule = discountRule;
        this.status = status;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.usedOrderId = usedOrderId;
        validate();
    }

    public static IssuedCoupon issue(CouponPolicy policy, Long memberId, MemberGrade grade, ZonedDateTime now) {
        policy.assertIssuable(now);
        ZonedDateTime expiresAt = now.toLocalDate()
            .plusDays(policy.getValidDays())
            .atStartOfDay(now.getZone());
        return new IssuedCoupon(null, policy.getId(), memberId, policy.getApplicabilityScope(),
            policy.resolveRuleFor(grade), CouponStatus.UNUSED, now, expiresAt, null);
    }

    public static IssuedCoupon reconstitute(Long id, Long policyId, Long memberId,
        ApplicabilityScope applicabilityScope, DiscountRule discountRule, CouponStatus status,
        ZonedDateTime issuedAt, ZonedDateTime expiresAt, Long usedOrderId) {
        return new IssuedCoupon(id, policyId, memberId, applicabilityScope, discountRule, status,
            issuedAt, expiresAt, usedOrderId);
    }

    /**
     * 적용 범위에 매칭되는 라인 부분합에 할인을 계산한다.
     *
     * @param resolvedCategoryIds CATEGORY 범위일 때 대상 카테고리의 서브트리 id 집합(application이 주문 시점에 해소).
     *                            다른 범위에서는 무시한다.
     */
    public Money calculateDiscount(List<DiscountableLine> lines, Set<Long> resolvedCategoryIds) {
        List<DiscountableLine> matched = lines.stream()
            .filter(line -> applicabilityScope.matches(line, resolvedCategoryIds))
            .toList();
        if (matched.isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "적용 대상 상품이 없습니다.");
        }
        Money base = matched.stream()
            .map(DiscountableLine::amount)
            .reduce(Money.ZERO, Money::plus);
        return discountRule.calculateDiscount(base);
    }

    public void use(Long memberId, List<DiscountableLine> lines, Set<Long> resolvedCategoryIds,
        ZonedDateTime now, Long orderId) {
        if (!this.memberId.equals(memberId)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰 소유자가 일치하지 않습니다.");
        }
        if (status != CouponStatus.UNUSED) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이미 사용된 쿠폰입니다.");
        }
        if (now == null || !now.isBefore(expiresAt)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "만료된 쿠폰입니다.");
        }
        if (orderId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰을 사용할 주문은 필수입니다.");
        }
        calculateDiscount(lines, resolvedCategoryIds);
        this.status = CouponStatus.USED;
        this.usedOrderId = orderId;
    }

    public void restore(Long orderId) {
        if (status != CouponStatus.USED) {
            return;
        }
        if (!usedOrderId.equals(orderId)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰을 사용한 주문이 일치하지 않습니다.");
        }
        this.status = CouponStatus.UNUSED;
        this.usedOrderId = null;
    }

    public boolean isExpired(ZonedDateTime now) {
        return now == null || !now.isBefore(expiresAt);
    }

    private void validate() {
        if (policyId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰 정책 ID는 필수입니다.");
        }
        if (memberId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰 소유자는 필수입니다.");
        }
        if (applicabilityScope == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "적용 범위는 필수입니다.");
        }
        if (discountRule == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "할인 규칙은 필수입니다.");
        }
        if (status == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰 상태는 필수입니다.");
        }
        if (issuedAt == null || expiresAt == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰 발급/만료 시각은 필수입니다.");
        }
        if (!issuedAt.isBefore(expiresAt)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰 만료 시각은 발급 시각보다 늦어야 합니다.");
        }
        if (status == CouponStatus.UNUSED && usedOrderId != null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "미사용 쿠폰은 사용 주문을 가질 수 없습니다.");
        }
        if (status == CouponStatus.USED && usedOrderId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용 쿠폰은 사용 주문이 필요합니다.");
        }
    }
}
