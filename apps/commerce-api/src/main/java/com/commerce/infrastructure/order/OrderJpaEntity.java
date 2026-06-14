package com.commerce.infrastructure.order;

import java.util.List;

import com.commerce.domain.order.Order;
import com.commerce.domain.order.OrderLine;
import com.commerce.domain.order.OrderStatus;
import com.commerce.domain.shared.Money;
import com.commerce.infrastructure.jpa.BaseJpaEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주문 루트 매핑. 주문 라인은 별도 테이블(order_lines)이라 여기 보유하지 않고,
 * OrderRepositoryImpl이 두 테이블을 조립한다. (JPA 연관 어노테이션 미사용)
 */
@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_orders_member_id", columnList = "member_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderJpaEntity extends BaseJpaEntity {

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false)
    private long totalAmount;

    @Column(name = "discount_amount", nullable = false)
    private long discountAmount;

    @Column(name = "payable_amount", nullable = false)
    private long payableAmount;

    @Column(name = "used_coupon_id")
    private Long usedCouponId;

    private OrderJpaEntity(Long memberId, OrderStatus status, long totalAmount, long discountAmount,
        long payableAmount, Long usedCouponId) {
        this.memberId = memberId;
        this.status = status;
        this.totalAmount = totalAmount;
        this.discountAmount = discountAmount;
        this.payableAmount = payableAmount;
        this.usedCouponId = usedCouponId;
    }

    public static OrderJpaEntity fromDomain(Order order) {
        return new OrderJpaEntity(order.getMemberId(), order.getStatus(), order.getTotalAmount().amount(),
            order.getDiscountAmount().amount(), order.getPayableAmount().amount(), order.getUsedCouponId());
    }

    /** 라인은 별도 테이블에서 로드해 넘겨준다. */
    public Order toDomain(List<OrderLine> orderLines) {
        return Order.reconstitute(getId(), memberId, status, orderLines, new Money(totalAmount),
            new Money(discountAmount), new Money(payableAmount), usedCouponId);
    }

    /** 상태 전이(markPaid/cancel)만 반영. memberId·총액·라인은 불변이라 건드리지 않는다. */
    public void updateFromDomain(Order order) {
        this.status = order.getStatus();
    }
}
