package com.commerce.domain.coupon;

/**
 * 쿠폰 할인의 적용 범위 종류.
 *
 * <p>{@code WHOLE}은 주문 전체, 나머지는 매칭되는 라인 부분합에만 할인을 적용한다.
 * {@code CATEGORY}는 대상 카테고리의 서브트리(자기 + 모든 하위)에 속한 상품을 매칭한다.
 */
public enum ScopeType {
    WHOLE,
    BRAND,
    PRODUCT,
    CATEGORY
}
