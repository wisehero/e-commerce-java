package com.commerce.application.order;

import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.commerce.domain.coupon.IssuedCouponRepository;
import com.commerce.domain.order.Order;
import com.commerce.domain.order.OrderLine;
import com.commerce.domain.order.OrderRepository;
import com.commerce.domain.order.OrderStatus;
import com.commerce.domain.order.PaymentGateway;
import com.commerce.domain.product.Sku;
import com.commerce.domain.product.SkuRepository;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

/**
 * 주문 취소 유스케이스. Txn(취소 + 재고 복원) → 환불 호출(트랜잭션 밖).
 * 환불은 결제됐던(PAID) 주문에 대해서만 한다.
 */
@Service
public class OrderCancelUseCase {

    private final OrderRepository orderRepository;
    private final SkuRepository skuRepository;
    private final IssuedCouponRepository issuedCouponRepository;
    private final PaymentGateway paymentGateway;
    private final TransactionTemplate transactionTemplate;

    public OrderCancelUseCase(OrderRepository orderRepository, SkuRepository skuRepository,
        IssuedCouponRepository issuedCouponRepository,
        PaymentGateway paymentGateway, PlatformTransactionManager transactionManager) {
        this.orderRepository = orderRepository;
        this.skuRepository = skuRepository;
        this.issuedCouponRepository = issuedCouponRepository;
        this.paymentGateway = paymentGateway;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public OrderInfo cancel(Long orderId) {
        AtomicBoolean wasPaid = new AtomicBoolean(false);

        // Txn: 취소(상태 전이) + 재고 복원
        Order cancelled = transactionTemplate.execute(status -> {
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 주문입니다."));
            wasPaid.set(order.getStatus() == OrderStatus.PAID);
            order.cancel();
            restoreStock(order);
            restoreCoupon(order);
            return orderRepository.save(order);
        });

        // 환불 — 결제됐던 주문만, 트랜잭션 밖
        if (wasPaid.get()) {
            paymentGateway.refund(cancelled.getId(), cancelled.getPayableAmount());
        }
        return OrderInfo.from(cancelled);
    }

    private void restoreStock(Order order) {
        for (OrderLine line : order.getOrderLines()) {
            Sku sku = skuRepository.findById(line.getSkuId())
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 SKU입니다."));
            sku.restock(line.getQuantity());
            skuRepository.save(sku);
        }
    }

    private void restoreCoupon(Order order) {
        if (order.getUsedCouponId() != null) {
            issuedCouponRepository.restoreByOrderId(order.getId());
        }
    }
}
