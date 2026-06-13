package com.commerce.application.product;

import java.util.List;

import com.commerce.domain.product.OptionValue;
import com.commerce.domain.product.Product;
import com.commerce.domain.product.Sku;

public record ProductDetailInfo(
    Long id,
    String name,
    String description,
    Long categoryId,
    Long brandId,
    String imageUrl,
    String status,
    List<SkuInfo> skus
) {
    public static ProductDetailInfo from(Product product, List<Sku> skus) {
        return new ProductDetailInfo(
            product.getId(),
            product.getName(),
            product.getDescription(),
            product.getCategoryId(),
            product.getBrandId(),
            product.getImageUrl(),
            product.getStatus().name(),
            skus.stream().map(SkuInfo::from).toList()
        );
    }

    public record SkuInfo(
        Long id,
        List<OptionValueInfo> optionValues,
        long originalPrice,
        long salePrice,
        int stock,
        boolean discounted
    ) {
        public static SkuInfo from(Sku sku) {
            return new SkuInfo(
                sku.getId(),
                sku.getOptionValues().stream().map(OptionValueInfo::from).toList(),
                sku.getOriginalPrice().amount(),
                sku.getSalePrice().amount(),
                sku.getStock().quantity(),
                sku.isDiscounted()
            );
        }
    }

    public record OptionValueInfo(String name, String value) {
        public static OptionValueInfo from(OptionValue ov) {
            return new OptionValueInfo(ov.name(), ov.value());
        }
    }
}
