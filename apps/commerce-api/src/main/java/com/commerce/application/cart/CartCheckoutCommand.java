package com.commerce.application.cart;

/**
 * 장바구니 체크아웃 입력. 현재는 member의 전체 카트를 주문으로 전환한다.
 */
public record CartCheckoutCommand(Long memberId, String lockMode, Long couponId) {
}
