package com.commerce.interfaces.api.product;

import com.commerce.application.product.SkuApplyDiscountCommand;

import jakarta.validation.constraints.PositiveOrZero;

public record SkuApplyDiscountRequest(
    @PositiveOrZero(message = "할인가는 0 이상이어야 합니다.")
    long salePrice
) {

    public SkuApplyDiscountCommand toCommand(Long skuId) {
        return new SkuApplyDiscountCommand(skuId, salePrice);
    }
}
