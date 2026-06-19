package com.commerce.domain.coupon;

import java.util.Set;

import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

/**
 * 쿠폰 할인이 어느 라인에 붙는지를 정의하는 Value Object.
 *
 * <p>{@code CouponPolicy}가 보유하고 발급 시 {@code IssuedCoupon}으로 스냅샷된다.
 * 한 쿠폰은 적용 범위를 하나만 가진다(단일 차원 + 단일 대상).
 *
 * <p>매칭은 두 형태다.
 * WHOLE/BRAND/PRODUCT는 라인 정보만으로 자족 판정한다.
 * CATEGORY는 대상 카테고리의 서브트리 id 집합이 필요한데, 카테고리 트리는 다른 Aggregate라
 * 순수 VO가 가질 수 없다. 따라서 application이 주문 시점에 해소한 집합({@code resolvedCategoryIds})을
 * 공급하면 멤버십으로 판정한다.
 */
public record ApplicabilityScope(ScopeType type, Long targetId) {

    public ApplicabilityScope {
        if (type == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "적용 범위 종류는 필수입니다.");
        }
        if (type == ScopeType.WHOLE && targetId != null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주문 전체 범위는 대상 ID를 가질 수 없습니다.");
        }
        if (type != ScopeType.WHOLE && targetId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "한정 범위는 대상 ID가 필수입니다.");
        }
    }

    public static ApplicabilityScope whole() {
        return new ApplicabilityScope(ScopeType.WHOLE, null);
    }

    public static ApplicabilityScope brand(Long brandId) {
        return new ApplicabilityScope(ScopeType.BRAND, brandId);
    }

    public static ApplicabilityScope product(Long productId) {
        return new ApplicabilityScope(ScopeType.PRODUCT, productId);
    }

    public static ApplicabilityScope category(Long categoryId) {
        return new ApplicabilityScope(ScopeType.CATEGORY, categoryId);
    }

    /**
     * 라인이 이 범위에 매칭되는지 판정한다.
     *
     * @param resolvedCategoryIds CATEGORY 범위일 때 대상 카테고리의 서브트리 id 집합.
     *                            다른 범위에서는 무시한다.
     */
    public boolean matches(DiscountableLine line, Set<Long> resolvedCategoryIds) {
        return switch (type) {
            case WHOLE -> true;
            case BRAND -> targetId.equals(line.brandId());
            case PRODUCT -> targetId.equals(line.productId());
            case CATEGORY -> resolvedCategoryIds != null && resolvedCategoryIds.contains(line.categoryId());
        };
    }
}
