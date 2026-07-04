package com.commerce.interfaces.api.product;

import io.swagger.v3.oas.annotations.Operation;

import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.commerce.application.product.SkuPriceChangeUseCase;
import com.commerce.application.product.SkuStockAdjustUseCase;
import com.commerce.interfaces.api.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/skus")
@RequiredArgsConstructor
public class SkuControllerV1 {

    private final SkuPriceChangeUseCase skuPriceChangeUseCase;
    private final SkuStockAdjustUseCase skuStockAdjustUseCase;

    @Operation(summary = "SKU 할인 적용")
    @PatchMapping("/{skuId}/discount")
    public ApiResponse<Object> applyDiscount(
        @PathVariable Long skuId,
        @Valid @RequestBody SkuApplyDiscountRequest request
    ) {
        skuPriceChangeUseCase.applyDiscount(request.toCommand(skuId));
        return ApiResponse.success();
    }

    @Operation(summary = "SKU 가격 변경")
    @PatchMapping("/{skuId}/price")
    public ApiResponse<Object> changePrice(
        @PathVariable Long skuId,
        @Valid @RequestBody SkuChangePriceRequest request
    ) {
        skuPriceChangeUseCase.changePrice(request.toCommand(skuId));
        return ApiResponse.success();
    }

    @Operation(summary = "SKU 재고 추가")
    @PatchMapping("/{skuId}/stock")
    public ApiResponse<Object> restock(
        @PathVariable Long skuId,
        @Valid @RequestBody SkuRestockRequest request
    ) {
        skuStockAdjustUseCase.restock(request.toCommand(skuId));
        return ApiResponse.success();
    }
}
