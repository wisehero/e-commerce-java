package com.commerce.interfaces.api.product;

import com.commerce.application.product.ProductSummaryInfo;

public record ProductSummaryResponse(
    Long id,
    String name,
    String imageUrl,
    long lowestSalePrice
) {

    public static ProductSummaryResponse from(ProductSummaryInfo info) {
        return new ProductSummaryResponse(
            info.id(), info.name(), info.imageUrl(), info.lowestSalePrice()
        );
    }
}
