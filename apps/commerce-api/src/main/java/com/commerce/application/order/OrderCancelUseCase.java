package com.commerce.application.order;

import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.commerce.domain.order.Order;
import com.commerce.domain.order.OrderRepository;
import com.commerce.domain.order.OrderStatus;
import com.commerce.domain.order.PaymentGateway;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

/**
 * 주문 취소 유스케이스. Txn(취소 + 재고 복원) → 환불 호출(트랜잭션 밖).
 * 환불은 결제됐던(PAID) 주문에 대해서만 한다.
 */
@Service
public class OrderCancelUseCase {

    private final OrderRepository orderRepository;
    private final OrderCompensationHelper orderCompensationHelper;
    private final PaymentGateway paymentGateway;
    private final TransactionTemplate transactionTemplate;

    public OrderCancelUseCase(OrderRepository orderRepository, OrderCompensationHelper orderCompensationHelper,
        PaymentGateway paymentGateway, PlatformTransactionManager transactionManager) {
        this.orderRepository = orderRepository;
        this.orderCompensationHelper = orderCompensationHelper;
        this.paymentGateway = paymentGateway;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public OrderInfo cancel(Long memberId, Long orderId) {
        AtomicBoolean wasPaid = new AtomicBoolean(false);

        // Txn: 취소(상태 전이) + 재고 복원
        Order cancelled = transactionTemplate.execute(status -> {
            Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 주문입니다."));
            ensureOwner(order, memberId);
            wasPaid.set(order.getStatus() == OrderStatus.PAID);
            order.cancel();
            orderCompensationHelper.restore(order);
            return orderRepository.save(order);
        });

        // 환불 — 결제됐던 주문만, 트랜잭션 밖
        if (wasPaid.get()) {
            paymentGateway.refund(cancelled.getId(), cancelled.getPayableAmount());
        }
        return OrderInfo.from(cancelled);
    }

    private void ensureOwner(Order order, Long memberId) {
        if (!order.getMemberId().equals(memberId)) {
            throw new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 주문입니다.");
        }
    }
}
