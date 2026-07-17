package com.commerce.application.order;

import java.time.ZonedDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.commerce.domain.cart.CartCleanupTask;
import com.commerce.domain.cart.CartCleanupTaskRepository;
import com.commerce.domain.coupon.DiscountableLine;
import com.commerce.domain.member.MemberRepository;
import com.commerce.domain.order.Order;
import com.commerce.domain.order.OrderLine;
import com.commerce.domain.order.OrderRepository;
import com.commerce.domain.order.OrderStatus;
import com.commerce.domain.order.PaymentGateway;
import com.commerce.domain.order.PaymentResult;
import com.commerce.domain.product.StockDeducter;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

import lombok.RequiredArgsConstructor;

/**
 * 주문 생성 유스케이스. 주문 생성 흐름을 orchestration 한다.
 *
 * <p>2단계 동기 흐름이다: Txn1(검증·라인 준비·재고차감·주문생성·쿠폰 사용) → 결제 호출(트랜잭션 밖) → Txn2(결제 결과 반영).
 * 외부 결제 호출이 재고 차감 락을 점유하지 않도록 트랜잭션 경계를 명시하려고 TransactionTemplate을 쓴다
 * (같은 빈 내 @Transactional 자기호출은 프록시를 거치지 않아 동작하지 않으므로).
 *
 * <p>라인 준비는 {@link OrderLinePreparer}, 쿠폰 적용은 {@link OrderCouponApplier}에 위임하고
 * 결제 실패 보상은 {@link OrderCompensationHelper}에 위임한다.
 */
@Service
@RequiredArgsConstructor
public class OrderPlaceUseCase {

    private final MemberRepository memberRepository;
    private final OrderRepository orderRepository;
    private final CartCleanupTaskRepository cartCleanupTaskRepository;
    private final OrderLinePreparer orderLinePreparer;
    private final OrderCouponApplier orderCouponApplier;
    private final OrderCompensationHelper orderCompensationHelper;
    private final PaymentGateway paymentGateway;
    private final TransactionTemplate transactionTemplate;

    public OrderInfo place(OrderPlaceCommand command) {
        // 전략 선택은 트랜잭션을 열기 전에 끝낸다(잘못된 lockMode면 트랜잭션 없이 즉시 거부).
        StockDeducter stockDeducter = orderLinePreparer.resolveStrategy(command.lockMode());

        // Txn1: 검증 + 라인 준비 + 재고 차감 + 주문(PAYMENT_PENDING) 저장 + 쿠폰 사용 → commit 시 SKU 락 해제
        Order pending = transactionTemplate.execute(status -> openPendingOrder(command, stockDeducter));

        // 결제 호출 — 트랜잭션 밖 (락 미점유)
        PaymentResult result = pending.getPayableAmount().isZero()
            ? PaymentResult.success()
            : paymentGateway.pay(pending.getId(), pending.getPayableAmount());

        // Txn2: 결제 결과 반영 (성공 → PAID / 실패 → CANCELLED + 재고 복원)
        Order settled = transactionTemplate.execute(status -> settlePayment(pending.getId(), result));

        return OrderInfo.from(settled);
    }

    private Order openPendingOrder(OrderPlaceCommand command, StockDeducter stockDeducter) {
        ensureMemberExists(command.memberId());

        List<PreparedLine> prepared = orderLinePreparer.prepare(command, stockDeducter);
        List<OrderLine> lines = prepared.stream().map(PreparedLine::orderLine).toList();
        List<DiscountableLine> discountableLines = prepared.stream().map(PreparedLine::discountableLine).toList();

        AppliedCoupon appliedCoupon = orderCouponApplier.apply(command.couponId(), command.memberId(),
            discountableLines);

        Order order = Order.place(command.memberId(), lines, appliedCoupon.discount(), command.couponId(),
            command.sourceCartId());
        Order saved = orderRepository.save(order);
        orderCouponApplier.markUsed(appliedCoupon, command.memberId(), saved.getId());
        return saved;
    }

    private void ensureMemberExists(Long memberId) {
        memberRepository.findById(memberId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 회원입니다."));
    }

    private Order settlePayment(Long orderId, PaymentResult result) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 주문입니다."));
        if (result.approved()) {
            order.markPaid();
        } else {
            order.cancel();
            orderCompensationHelper.restore(order);
        }
        Order saved = orderRepository.save(order);
        if (saved.getStatus() == OrderStatus.PAID && saved.getSourceCartId() != null) {
            cartCleanupTaskRepository.save(CartCleanupTask.pending(saved.getId(), saved.getSourceCartId(),
                saved.getMemberId(), ZonedDateTime.now()));
        }
        return saved;
    }
}
