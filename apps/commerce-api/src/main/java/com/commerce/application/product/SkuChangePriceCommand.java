package com.commerce.application.product;

public record SkuChangePriceCommand(
    Long skuId,
    long originalPrice
) {
}
