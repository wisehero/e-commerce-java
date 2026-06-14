package com.commerce.interfaces.api.order;

import java.util.List;

import com.commerce.application.order.OrderInfo;
import com.commerce.application.order.OrderLineInfo;

public record OrderResponse(
    Long id,
    Long memberId,
    String status,
    List<OrderLineResponse> lines,
    long totalAmount,
    long discountAmount,
    long payableAmount,
    Long usedCouponId
) {

    public static OrderResponse from(OrderInfo info) {
        return new OrderResponse(
            info.id(),
            info.memberId(),
            info.status(),
            info.lines().stream().map(OrderLineResponse::from).toList(),
            info.totalAmount(),
            info.discountAmount(),
            info.payableAmount(),
            info.usedCouponId()
        );
    }

    public record OrderLineResponse(
        Long id,
        Long productId,
        Long skuId,
        String productName,
        String optionSummary,
        long unitPrice,
        int quantity,
        long lineAmount
    ) {
        public static OrderLineResponse from(OrderLineInfo info) {
            return new OrderLineResponse(
                info.id(), info.productId(), info.skuId(), info.productName(),
                info.optionSummary(), info.unitPrice(), info.quantity(), info.lineAmount()
            );
        }
    }
}
