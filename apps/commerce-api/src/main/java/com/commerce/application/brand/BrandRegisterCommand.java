package com.commerce.application.brand;

public record BrandRegisterCommand(
    String name,
    String logoUrl
) {
}
