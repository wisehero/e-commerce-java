package com.commerce.interfaces.api.product;

import com.commerce.application.product.SkuRestockCommand;

import jakarta.validation.constraints.Positive;

public record SkuRestockRequest(
    @Positive(message = "입고 수량은 1 이상이어야 합니다.")
    int quantity
) {

    public SkuRestockCommand toCommand(Long skuId) {
        return new SkuRestockCommand(skuId, quantity);
    }
}
