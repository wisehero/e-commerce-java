package com.commerce.application.order;

import java.util.List;
import java.util.Set;

import com.commerce.domain.coupon.DiscountableLine;
import com.commerce.domain.coupon.IssuedCoupon;
import com.commerce.domain.shared.Money;

/**
 * 쿠폰 적용 중간 결과. 저장 전에 계산한 할인액과, 저장 후 사용 처리에 필요한
 * 쿠폰·할인 대상 라인·해소된 카테고리 집합을 함께 들고 다닌다.
 * 쿠폰이 없으면 coupon은 null, discount는 0원이다.
 */
record AppliedCoupon(
    IssuedCoupon coupon,
    Money discount,
    List<DiscountableLine> discountableLines,
    Set<Long> resolvedCategoryIds
) {
}
