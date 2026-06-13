package com.commerce.application.cart;

import java.util.List;

/**
 * 장바구니 조회 결과. cartTotal은 PURCHASABLE 라인 소계의 합이다(구매 불가 라인 제외).
 */
public record CartInfo(
    Long memberId,
    List<CartLineInfo> lines,
    long cartTotal
) {
}
