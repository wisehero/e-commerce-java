package com.commerce.interfaces.api.brand;

import com.commerce.application.brand.BrandInfo;

public record BrandResponse(
    Long id,
    String name,
    String logoUrl,
    String status
) {
    public static BrandResponse from(BrandInfo info) {
        return new BrandResponse(
            info.id(),
            info.name(),
            info.logoUrl(),
            info.status()
        );
    }
}
