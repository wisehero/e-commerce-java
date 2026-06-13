package com.commerce.application.product;

public record SkuApplyDiscountCommand(
    Long skuId,
    long salePrice
) {
}
