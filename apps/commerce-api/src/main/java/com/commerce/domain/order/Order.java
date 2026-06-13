package com.commerce.domain.order;

import java.util.List;

import com.commerce.domain.shared.Money;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

import lombok.Getter;

/**
 * 주문 Aggregate Root.
 *
 * <p>orderLines를 자식 엔티티로 소유하고, 총액을 라인 합으로 스스로 계산해 보유한다(외부에서 총액을 받지 않는다).
 * 상태 전이(PAYMENT_PENDING → PAID/CANCELLED) 규칙을 소유한다.
 */
@Getter
public class Order {

    private Long id;
    private Long memberId;
    private OrderStatus status;
    private List<OrderLine> orderLines;
    private Money totalAmount;

    private Order(Long id, Long memberId, OrderStatus status, List<OrderLine> orderLines, Money totalAmount) {
        this.id = id;
        this.memberId = memberId;
        this.status = status;
        this.orderLines = (orderLines == null) ? null : List.copyOf(orderLines);
        this.totalAmount = totalAmount;
        validate();
    }

    /** 신규 주문: 결제 대기 상태로 생성, 총액은 라인 합으로 스스로 계산한다. */
    public static Order place(Long memberId, List<OrderLine> orderLines) {
        return new Order(null, memberId, OrderStatus.PAYMENT_PENDING, orderLines, calculateTotal(orderLines));
    }

    /** 영속 복원: 저장된 총액(주문 당시 청구액)을 그대로 복원한다. */
    public static Order reconstitute(Long id, Long memberId, OrderStatus status,
        List<OrderLine> orderLines, Money totalAmount) {
        return new Order(id, memberId, status, orderLines, totalAmount);
    }

    private void validate() {
        if (memberId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주문자는 필수입니다.");
        }
        if (status == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주문 상태는 필수입니다.");
        }
        if (orderLines == null || orderLines.isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주문은 최소 1개의 주문 항목을 가져야 합니다.");
        }
        if (totalAmount == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주문 총액은 필수입니다.");
        }
    }

    private static Money calculateTotal(List<OrderLine> orderLines) {
        if (orderLines == null) {
            return Money.ZERO;                 // 빈 주문은 validate()가 막는다
        }
        return orderLines.stream()
            .map(OrderLine::lineAmount)
            .reduce(Money.ZERO, Money::plus);
    }

    public void markPaid() {
        if (status != OrderStatus.PAYMENT_PENDING) {
            throw new CoreException(ErrorType.BAD_REQUEST, "결제 대기 중인 주문만 결제 완료 처리할 수 있습니다.");
        }
        this.status = OrderStatus.PAID;
    }

    public void cancel() {
        if (status == OrderStatus.CANCELLED) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이미 취소된 주문입니다.");
        }
        this.status = OrderStatus.CANCELLED;
    }
}
