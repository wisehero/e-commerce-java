package com.commerce.interfaces.api.product;

import com.commerce.application.product.SkuChangePriceCommand;

import jakarta.validation.constraints.PositiveOrZero;

public record SkuChangePriceRequest(
    @PositiveOrZero(message = "정가는 0 이상이어야 합니다.")
    long originalPrice
) {

    public SkuChangePriceCommand toCommand(Long skuId) {
        return new SkuChangePriceCommand(skuId, originalPrice);
    }
}
