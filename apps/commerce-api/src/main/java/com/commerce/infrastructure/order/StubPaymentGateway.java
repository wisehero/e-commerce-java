package com.commerce.infrastructure.order;

import org.springframework.stereotype.Component;

import com.commerce.domain.order.PaymentGateway;
import com.commerce.domain.order.PaymentResult;
import com.commerce.domain.shared.Money;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

/**
 * 결제 게이트웨이 스텁. 외부 PG 호출 지연을 sleep으로 흉내 내고 항상 성공한다.
 * 실패→보상 경로는 테스트에서 실패하는 PaymentGateway 목으로 검증한다.
 */
@Component
public class StubPaymentGateway implements PaymentGateway {

    private static final long LATENCY_MILLIS = 200L;

    @Override
    public PaymentResult pay(Long orderId, Money amount) {
        simulateLatency();
        return PaymentResult.success();
    }

    @Override
    public void refund(Long orderId, Money amount) {
        simulateLatency();
    }

    private void simulateLatency() {
        try {
            Thread.sleep(LATENCY_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CoreException(ErrorType.INTERNAL_ERROR, "결제 처리가 중단되었습니다.");
        }
    }
}
