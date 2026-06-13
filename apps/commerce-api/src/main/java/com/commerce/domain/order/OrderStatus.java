package com.commerce.domain.order;

/**
 * 주문 상태.
 *
 * <p>PAYMENT_PENDING(생성·재고차감 완료, 결제 결과 대기) → PAID(결제 성공) / CANCELLED(결제 실패 보상 또는 유저 취소).
 * CANCELLED는 단말 상태다. 상태 전이 규칙은 {@link Order}가 소유한다.
 */
public enum OrderStatus {
    PAYMENT_PENDING,
    PAID,
    CANCELLED
}
