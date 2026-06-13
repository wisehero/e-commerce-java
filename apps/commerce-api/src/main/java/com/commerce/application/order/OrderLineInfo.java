package com.commerce.application.order;

import com.commerce.domain.order.OrderLine;

public record OrderLineInfo(
    Long id,
    Long productId,
    Long skuId,
    String productName,
    String optionSummary,
    long unitPrice,
    int quantity,
    long lineAmount
) {

    public static OrderLineInfo from(OrderLine line) {
        return new OrderLineInfo(
            line.getId(), line.getProductId(), line.getSkuId(), line.getProductName(),
            line.getOptionSummary(), line.getUnitPrice().amount(), line.getQuantity(),
            line.lineAmount().amount()
        );
    }
}
