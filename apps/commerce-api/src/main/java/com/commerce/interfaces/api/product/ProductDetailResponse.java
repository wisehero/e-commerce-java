package com.commerce.interfaces.api.product;

import static com.commerce.application.product.ProductDetailInfo.*;

import java.util.List;

import com.commerce.application.product.ProductDetailInfo;

public record ProductDetailResponse(
    Long id,
    String name,
    String description,
    Long categoryId,
    Long brandId,
    String brandName,
    String brandLogoUrl,
    String imageUrl,
    String status,
    List<SkuResponse> skus
) {

    public static ProductDetailResponse from(ProductDetailInfo info) {
        return new ProductDetailResponse(
            info.id(), info.name(), info.description(), info.categoryId(),
            info.brandId(), info.brandName(), info.brandLogoUrl(), info.imageUrl(), info.status(),
            info.skus().stream().map(SkuResponse::from).toList()
        );
    }

    public record SkuResponse(
        Long id,
        List<OptionValueResponse> optionValues,
        long originalPrice,
        long salePrice,
        int stock,
        boolean discounted
    ) {
        public static SkuResponse from(SkuInfo info) {
            return new SkuResponse(
                info.id(),
                info.optionValues().stream().map(OptionValueResponse::from).toList(),
                info.originalPrice(), info.salePrice(), info.stock(), info.discounted()
            );
        }
    }

    public record OptionValueResponse(String name, String value) {
        public static OptionValueResponse from(OptionValueInfo info) {
            return new OptionValueResponse(info.name(), info.value());
        }
    }
}
