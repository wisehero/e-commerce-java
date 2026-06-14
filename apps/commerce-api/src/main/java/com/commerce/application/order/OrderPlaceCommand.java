package com.commerce.application.order;

import java.util.List;

/**
 * 주문 생성 입력. lockMode로 재고 차감 전략을 런타임 선택한다(optimistic/pessimistic/atomic).
 */
public record OrderPlaceCommand(Long memberId, List<LineCommand> lines, String lockMode, Long couponId) {

    public OrderPlaceCommand(Long memberId, List<LineCommand> lines, String lockMode) {
        this(memberId, lines, lockMode, null);
    }

    public record LineCommand(Long skuId, int quantity) {
    }
}
