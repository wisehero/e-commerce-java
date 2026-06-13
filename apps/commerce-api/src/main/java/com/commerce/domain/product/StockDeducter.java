package com.commerce.domain.product;

/**
 * SKU 판매 가능 재고를 차감하는 포트. 오버셀 방지를 위한 동시성 제어 전략이
 * 구현체별로 다르다(낙관적 락 / 비관적 락 / 조건부 원자 UPDATE).
 *
 * <p>주문 생성 시 application이 lockMode로 구현체를 선택해 호출한다.
 * 재고 부족이면 {@code BAD_REQUEST}, 미존재면 {@code NOT_FOUND},
 * 동시성 충돌이면 {@code CONFLICT}를 던진다.
 */
public interface StockDeducter {

    void deduct(Long skuId, int quantity);
}
