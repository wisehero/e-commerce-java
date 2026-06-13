package com.commerce.application.product;

public record SkuRestockCommand(
    Long skuId,
    int quantity
) {
}
