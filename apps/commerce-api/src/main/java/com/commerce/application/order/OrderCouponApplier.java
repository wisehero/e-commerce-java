package com.commerce.application.order;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.commerce.domain.category.CategoryRepository;
import com.commerce.domain.coupon.ApplicabilityScope;
import com.commerce.domain.coupon.DiscountableLine;
import com.commerce.domain.coupon.IssuedCoupon;
import com.commerce.domain.coupon.IssuedCouponRepository;
import com.commerce.domain.coupon.ScopeType;
import com.commerce.domain.shared.Money;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

/**
 * 주문 쿠폰 적용 담당. 저장 전 단계(쿠폰 조회·카테고리 해소·할인 계산)와
 * 저장 후 단계(쿠폰 사용 처리)로 나뉜다. orderId가 주문 저장 후에야 정해지므로
 * 두 단계가 공유하는 중간 결과는 AppliedCoupon에 담아 넘긴다.
 */
@Component
public class OrderCouponApplier {

    private final IssuedCouponRepository issuedCouponRepository;
    private final CategoryRepository categoryRepository;

    public OrderCouponApplier(IssuedCouponRepository issuedCouponRepository, CategoryRepository categoryRepository) {
        this.issuedCouponRepository = issuedCouponRepository;
        this.categoryRepository = categoryRepository;
    }

    /** 저장 전: 쿠폰이 없으면 할인 0원. 있으면 카테고리 서브트리를 신선 해소해 할인액을 계산한다. */
    public AppliedCoupon apply(Long couponId, Long memberId, List<DiscountableLine> discountableLines) {
        IssuedCoupon coupon = findCoupon(couponId);
        ensureOwner(coupon, memberId);
        Set<Long> resolvedCategoryIds = resolveCategoryIds(coupon);
        Money discount = coupon == null
            ? Money.ZERO
            : coupon.calculateDiscount(discountableLines, resolvedCategoryIds);
        return new AppliedCoupon(coupon, discount, discountableLines, resolvedCategoryIds);
    }

    /** 저장 후: 쿠폰 사용 처리. 기존 순서·동작(도메인 use → 조건부 원자 마킹)을 그대로 유지한다. */
    public void markUsed(AppliedCoupon appliedCoupon, Long memberId, Long orderId) {
        IssuedCoupon coupon = appliedCoupon.coupon();
        if (coupon == null) {
            return;
        }
        ZonedDateTime now = ZonedDateTime.now();
        coupon.use(memberId, appliedCoupon.discountableLines(), appliedCoupon.resolvedCategoryIds(), now, orderId);
        if (!issuedCouponRepository.markUsedIfAvailable(coupon.getId(), memberId, now, orderId)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이미 사용했거나 만료된 쿠폰입니다.");
        }
    }

    private IssuedCoupon findCoupon(Long couponId) {
        if (couponId == null) {
            return null;
        }
        return issuedCouponRepository.findById(couponId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 쿠폰입니다."));
    }

    private void ensureOwner(IssuedCoupon coupon, Long memberId) {
        if (coupon != null && !coupon.getMemberId().equals(memberId)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰 소유자가 일치하지 않습니다.");
        }
    }

    /** CATEGORY scope면 대상 카테고리의 서브트리 id 집합을 주문 시점에 신선 해소한다. 그 외엔 빈 집합. */
    private Set<Long> resolveCategoryIds(IssuedCoupon coupon) {
        if (coupon == null) {
            return Set.of();
        }
        ApplicabilityScope scope = coupon.getApplicabilityScope();
        if (scope.type() == ScopeType.CATEGORY) {
            return Set.copyOf(categoryRepository.findSelfAndDescendantIds(scope.targetId()));
        }
        return Set.of();
    }
}
