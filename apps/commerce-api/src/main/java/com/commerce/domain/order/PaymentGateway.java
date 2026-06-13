package com.commerce.domain.order;

import com.commerce.domain.shared.Money;

/**
 * 결제 외부 시스템 포트. 결제 호출은 무거우므로 재고 차감 트랜잭션 밖에서 호출한다.
 * (구현은 infrastructure — 현재는 sleep 스텁)
 */
public interface PaymentGateway {

    PaymentResult pay(Long orderId, Money amount);

    void refund(Long orderId, Money amount);
}
