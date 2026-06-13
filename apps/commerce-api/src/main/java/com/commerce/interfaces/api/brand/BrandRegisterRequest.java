package com.commerce.interfaces.api.brand;

import com.commerce.application.brand.BrandRegisterCommand;

import jakarta.validation.constraints.NotBlank;

public record BrandRegisterRequest(
    @NotBlank(message = "브랜드명은 필수입니다.")
    String name,

    String logoUrl
) {
    public BrandRegisterCommand toCommand() {
        return new BrandRegisterCommand(name, logoUrl);
    }
}
