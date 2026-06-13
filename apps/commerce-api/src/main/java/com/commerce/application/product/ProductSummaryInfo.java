package com.commerce.application.product;

import com.commerce.domain.product.Product;

public record ProductSummaryInfo(
    Long id,
    String name,
    String imageUrl,
    long lowestSalePrice
) {
    public static ProductSummaryInfo of(Product product, long lowestSalePrice) {
        return new ProductSummaryInfo(
            product.getId(), product.getName(), product.getImageUrl(), lowestSalePrice
        );
    }
}
