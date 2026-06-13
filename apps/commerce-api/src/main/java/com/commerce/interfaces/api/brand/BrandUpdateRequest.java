package com.commerce.interfaces.api.brand;

import com.commerce.application.brand.BrandUpdateCommand;

import jakarta.validation.constraints.NotBlank;

public record BrandUpdateRequest(
    @NotBlank(message = "브랜드명은 필수입니다.")
    String name,

    String logoUrl
) {
    public BrandUpdateCommand toCommand(Long brandId) {
        return new BrandUpdateCommand(brandId, name, logoUrl);
    }
}
