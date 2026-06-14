package com.commerce.application.order;

import java.util.List;

import com.commerce.domain.order.Order;

public record OrderInfo(
    Long id,
    Long memberId,
    String status,
    List<OrderLineInfo> lines,
    long totalAmount,
    long discountAmount,
    long payableAmount,
    Long usedCouponId
) {

    public OrderInfo(Long id, Long memberId, String status, List<OrderLineInfo> lines, long totalAmount) {
        this(id, memberId, status, lines, totalAmount, 0, totalAmount, null);
    }

    public static OrderInfo from(Order order) {
        return new OrderInfo(
            order.getId(),
            order.getMemberId(),
            order.getStatus().name(),
            order.getOrderLines().stream().map(OrderLineInfo::from).toList(),
            order.getTotalAmount().amount(),
            order.getDiscountAmount().amount(),
            order.getPayableAmount().amount(),
            order.getUsedCouponId()
        );
    }
}
