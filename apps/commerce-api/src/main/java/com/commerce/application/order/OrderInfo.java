package com.commerce.application.order;

import java.util.List;

import com.commerce.domain.order.Order;

public record OrderInfo(
    Long id,
    Long memberId,
    String status,
    List<OrderLineInfo> lines,
    long totalAmount
) {

    public static OrderInfo from(Order order) {
        return new OrderInfo(
            order.getId(),
            order.getMemberId(),
            order.getStatus().name(),
            order.getOrderLines().stream().map(OrderLineInfo::from).toList(),
            order.getTotalAmount().amount()
        );
    }
}
