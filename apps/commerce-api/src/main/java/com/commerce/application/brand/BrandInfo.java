package com.commerce.application.brand;

import com.commerce.domain.brand.Brand;

public record BrandInfo(
    Long id,
    String name,
    String logoUrl,
    String status
) {
    public static BrandInfo from(Brand brand) {
        return new BrandInfo(
            brand.getId(),
            brand.getName(),
            brand.getLogoUrl(),
            brand.getStatus().name()
        );
    }
}
