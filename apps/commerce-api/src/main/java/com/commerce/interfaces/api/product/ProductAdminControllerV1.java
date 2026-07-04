package com.commerce.interfaces.api.product;

import io.swagger.v3.oas.annotations.Operation;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.commerce.application.product.ProductDetailInfo;
import com.commerce.application.product.ProductRegisterUseCase;
import com.commerce.application.product.ProductStatusChangeUseCase;
import com.commerce.interfaces.api.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/products")
@RequiredArgsConstructor
public class ProductAdminControllerV1 {

    private final ProductRegisterUseCase productRegisterUseCase;
    private final ProductStatusChangeUseCase productStatusChangeUseCase;

    @Operation(summary = "상품 등록")
    @PostMapping
    public ApiResponse<ProductDetailResponse> register(@Valid @RequestBody ProductRegisterRequest request) {
        ProductDetailInfo info = productRegisterUseCase.register(request.toCommand());
        return ApiResponse.success(ProductDetailResponse.from(info));
    }

    @Operation(summary = "상품 판매 중지")
    @PostMapping("/{productId}/suspend")
    public ApiResponse<Object> suspend(@PathVariable Long productId) {
        productStatusChangeUseCase.suspend(productId);
        return ApiResponse.success();
    }

    @Operation(summary = "상품 판매 재개")
    @PostMapping("/{productId}/resume")
    public ApiResponse<Object> resume(@PathVariable Long productId) {
        productStatusChangeUseCase.resume(productId);
        return ApiResponse.success();
    }

    @Operation(summary = "상품 단종")
    @PostMapping("/{productId}/discontinue")
    public ApiResponse<Object> discontinue(@PathVariable Long productId) {
        productStatusChangeUseCase.discontinue(productId);
        return ApiResponse.success();
    }
}
