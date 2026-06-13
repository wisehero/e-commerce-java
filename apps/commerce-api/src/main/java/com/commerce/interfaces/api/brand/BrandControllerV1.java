package com.commerce.interfaces.api.brand;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.commerce.application.brand.BrandInfo;
import com.commerce.application.brand.BrandQueryUseCase;
import com.commerce.application.brand.BrandRegisterUseCase;
import com.commerce.application.brand.BrandStatusChangeUseCase;
import com.commerce.application.brand.BrandUpdateUseCase;
import com.commerce.interfaces.api.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/brands")
@RequiredArgsConstructor
public class BrandControllerV1 {

    private final BrandQueryUseCase brandQueryUseCase;
    private final BrandRegisterUseCase brandRegisterUseCase;
    private final BrandUpdateUseCase brandUpdateUseCase;
    private final BrandStatusChangeUseCase brandStatusChangeUseCase;

    @GetMapping("/{brandId}")
    public ApiResponse<BrandResponse> getBrand(@PathVariable Long brandId) {
        BrandInfo info = brandQueryUseCase.getBrand(brandId);
        return ApiResponse.success(BrandResponse.from(info));
    }

    @PostMapping
    public ApiResponse<BrandResponse> register(@Valid @RequestBody BrandRegisterRequest request) {
        BrandInfo info = brandRegisterUseCase.register(request.toCommand());
        return ApiResponse.success(BrandResponse.from(info));
    }

    @PatchMapping("/{brandId}")
    public ApiResponse<BrandResponse> update(
        @PathVariable Long brandId,
        @Valid @RequestBody BrandUpdateRequest request
    ) {
        BrandInfo info = brandUpdateUseCase.update(request.toCommand(brandId));
        return ApiResponse.success(BrandResponse.from(info));
    }

    @PostMapping("/{brandId}/activate")
    public ApiResponse<Object> activate(@PathVariable Long brandId) {
        brandStatusChangeUseCase.activate(brandId);
        return ApiResponse.success();
    }

    @PostMapping("/{brandId}/deactivate")
    public ApiResponse<Object> deactivate(@PathVariable Long brandId) {
        brandStatusChangeUseCase.deactivate(brandId);
        return ApiResponse.success();
    }
}
