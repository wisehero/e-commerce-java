package com.commerce.application.brand;

public record BrandUpdateCommand(
    Long brandId,
    String name,
    String logoUrl
) {
}
