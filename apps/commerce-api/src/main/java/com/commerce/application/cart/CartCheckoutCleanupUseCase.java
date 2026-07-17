package com.commerce.application.cart;

import java.time.ZonedDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.commerce.domain.cart.Cart;
import com.commerce.domain.cart.CartCleanupTask;
import com.commerce.domain.cart.CartCleanupTaskRepository;
import com.commerce.domain.cart.CartRepository;
import com.commerce.domain.order.Order;
import com.commerce.domain.order.OrderRepository;
import com.commerce.domain.order.OrderStatus;

import lombok.extern.slf4j.Slf4j;

/**
 * PAID 주문의 라인 수량만 카트에서 차감하고 정리 작업을 완료한다.
 * 실패는 결제 결과를 뒤집지 않고 PENDING 작업에 기록해 다음 실행에서 재시도한다.
 */
@Slf4j
@Service
public class CartCheckoutCleanupUseCase {

    private static final int MAX_ERROR_LENGTH = 1000;

    private final CartCleanupTaskRepository cleanupTaskRepository;
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final TransactionTemplate transactionTemplate;

    public CartCheckoutCleanupUseCase(CartCleanupTaskRepository cleanupTaskRepository, CartRepository cartRepository,
        OrderRepository orderRepository, PlatformTransactionManager transactionManager) {
        this.cleanupTaskRepository = cleanupTaskRepository;
        this.cartRepository = cartRepository;
        this.orderRepository = orderRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /** 결제 직후 orderId의 정리를 한 번 시도한다. 실패해도 예외를 외부로 전파하지 않는다. */
    public boolean cleanup(Long orderId) {
        try {
            return cleanupTaskRepository.findByOrderId(orderId)
                .map(task -> attempt(task.getId()))
                .orElseGet(() -> {
                    log.error("PAID cart order has no cleanup task. orderId={}", orderId);
                    return false;
                });
        } catch (RuntimeException exception) {
            // 작업은 결제 결과와 같은 트랜잭션에서 이미 저장됐다. 조회 장애는 스케줄러가 복구하도록 맡긴다.
            log.error("Failed to start cart checkout cleanup. orderId={}", orderId, exception);
            return false;
        }
    }

    /** 스케줄러 진입점. 실행 시각이 된 PENDING 작업을 각각 독립 트랜잭션으로 처리한다. */
    public void retryReady() {
        cleanupTaskRepository.findReady(ZonedDateTime.now())
            .forEach(task -> attempt(task.getId()));
    }

    /** 이전 결제의 미완료 정리를 즉시 재시도한다. 모두 끝나야 새 체크아웃을 허용한다. */
    public boolean cleanupPending(Long memberId) {
        cleanupTaskRepository.findPendingByMemberId(memberId)
            .forEach(task -> attempt(task.getId()));
        return !cleanupTaskRepository.existsPendingByMemberId(memberId);
    }

    private boolean attempt(Long taskId) {
        try {
            return Boolean.TRUE.equals(transactionTemplate.execute(status -> execute(taskId)));
        } catch (RuntimeException exception) {
            recordFailure(taskId, exception);
            log.error("Cart checkout cleanup failed. taskId={}", taskId, exception);
            return false;
        }
    }

    private boolean execute(Long taskId) {
        CartCleanupTask task = cleanupTaskRepository.findByIdForUpdate(taskId)
            .orElseThrow(() -> new IllegalStateException("Cart cleanup task not found: " + taskId));
        if (task.isCompleted()) {
            return true;
        }

        Order order = orderRepository.findByIdForUpdate(task.getOrderId())
            .orElseThrow(() -> new IllegalStateException("Order not found for cart cleanup: " + task.getOrderId()));
        validateTaskOwner(task, order);
        if (order.getStatus() == OrderStatus.CANCELLED) {
            complete(task);
            return true;
        }
        if (order.getStatus() != OrderStatus.PAID) {
            throw new IllegalStateException("Only settled orders can finish cart cleanup: " + order.getId());
        }

        Cart cart = cartRepository.findByIdForUpdate(task.getCartId()).orElse(null);
        if (cart != null) {
            if (!cart.getMemberId().equals(task.getMemberId())) {
                throw new IllegalStateException("Cart cleanup owner mismatch: " + taskId);
            }
            order.getOrderLines()
                .forEach(line -> cart.removePurchasedItem(line.getSkuId(), line.getQuantity()));
            cartRepository.save(cart);
        }

        complete(task);
        return true;
    }

    private void validateTaskOwner(CartCleanupTask task, Order order) {
        if (!task.getMemberId().equals(order.getMemberId())
            || !task.getCartId().equals(order.getSourceCartId())) {
            throw new IllegalStateException("Cart cleanup task does not match order: " + order.getId());
        }
    }

    private void complete(CartCleanupTask task) {
        task.complete(ZonedDateTime.now());
        cleanupTaskRepository.save(task);
    }

    private void recordFailure(Long taskId, RuntimeException exception) {
        try {
            transactionTemplate.executeWithoutResult(status -> cleanupTaskRepository.findByIdForUpdate(taskId)
                .filter(CartCleanupTask::isPending)
                .ifPresent(task -> {
                    task.recordFailure(ZonedDateTime.now(), errorMessage(exception));
                    cleanupTaskRepository.save(task);
                }));
        } catch (RuntimeException recordingFailure) {
            log.error("Failed to record cart cleanup failure. taskId={}", taskId, recordingFailure);
        }
    }

    private String errorMessage(RuntimeException exception) {
        String message = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
        return message.length() <= MAX_ERROR_LENGTH ? message : message.substring(0, MAX_ERROR_LENGTH);
    }
}
