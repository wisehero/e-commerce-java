package com.commerce.domain.coupon;

import java.time.ZonedDateTime;

import com.commerce.domain.shared.Money;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

import lombok.Getter;

@Getter
public class IssuedCoupon {

    private Long id;
    private Long policyId;
    private Long memberId;
    private DiscountRule discountRule;
    private CouponStatus status;
    private ZonedDateTime issuedAt;
    private ZonedDateTime expiresAt;
    private Long usedOrderId;

    private IssuedCoupon(Long id, Long policyId, Long memberId, DiscountRule discountRule, CouponStatus status,
        ZonedDateTime issuedAt, ZonedDateTime expiresAt, Long usedOrderId) {
        this.id = id;
        this.policyId = policyId;
        this.memberId = memberId;
        this.discountRule = discountRule;
        this.status = status;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.usedOrderId = usedOrderId;
        validate();
    }

    public static IssuedCoupon issue(CouponPolicy policy, Long memberId, ZonedDateTime now) {
        policy.assertIssuable(now);
        ZonedDateTime expiresAt = now.toLocalDate()
            .plusDays(policy.getValidDays())
            .atStartOfDay(now.getZone());
        return new IssuedCoupon(null, policy.getId(), memberId, policy.getDiscountRule(), CouponStatus.UNUSED,
            now, expiresAt, null);
    }

    public static IssuedCoupon reconstitute(Long id, Long policyId, Long memberId, DiscountRule discountRule,
        CouponStatus status, ZonedDateTime issuedAt, ZonedDateTime expiresAt, Long usedOrderId) {
        return new IssuedCoupon(id, policyId, memberId, discountRule, status, issuedAt, expiresAt, usedOrderId);
    }

    public Money calculateDiscount(Money orderAmount) {
        return discountRule.calculateDiscount(orderAmount);
    }

    public void use(Long memberId, Money orderAmount, ZonedDateTime now, Long orderId) {
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
        calculateDiscount(orderAmount);
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
