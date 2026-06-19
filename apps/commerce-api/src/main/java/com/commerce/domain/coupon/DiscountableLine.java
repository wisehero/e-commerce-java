package com.commerce.domain.coupon;

import com.commerce.domain.shared.Money;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

/**
 * 할인 계산을 위한 중립 라인 입력.
 *
 * <p>쿠폰 도메인이 주문(다른 Aggregate)의 {@code OrderLine}을 직접 알지 않도록 막는 경계 차단막이다.
 * 영속 대상이 아니라 주문 시점에 application이 상품 정보로 즉석 조립해 넘기고 버린다.
 * {@code categoryId}는 상품의 리프 카테고리 id이며, CATEGORY scope의 서브트리 매칭에 쓰인다.
 */
public record DiscountableLine(
    Money amount,
    Long productId,
    Long brandId,
    Long categoryId
) {

    public DiscountableLine {
        if (amount == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "라인 금액은 필수입니다.");
        }
        if (productId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품 ID는 필수입니다.");
        }
        if (brandId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "브랜드 ID는 필수입니다.");
        }
        if (categoryId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "카테고리 ID는 필수입니다.");
        }
    }
}
