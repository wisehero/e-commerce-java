package com.commerce.interfaces.api.brand;

import io.swagger.v3.oas.annotations.Operation;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.commerce.application.brand.BrandInfo;
import com.commerce.application.brand.BrandQueryUseCase;
import com.commerce.interfaces.api.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/brands")
@RequiredArgsConstructor
public class BrandControllerV1 {

    private final BrandQueryUseCase brandQueryUseCase;

    @Operation(summary = "브랜드 상세 조회")
    @GetMapping("/{brandId}")
    public ApiResponse<BrandResponse> getBrand(@PathVariable Long brandId) {
        BrandInfo info = brandQueryUseCase.getBrand(brandId);
        return ApiResponse.success(BrandResponse.from(info));
    }
}
