package com.commerce.interfaces.api.product;

import io.swagger.v3.oas.annotations.Operation;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.commerce.application.product.ProductDetailInfo;
import com.commerce.application.product.ProductDetailQueryUseCase;
import com.commerce.application.product.ProductSearchUseCase;
import com.commerce.application.product.ProductSummaryInfo;
import com.commerce.interfaces.api.ApiResponse;
import com.commerce.interfaces.api.PageResponse;
import com.commerce.support.page.PageResult;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductControllerV1 {

    private final ProductDetailQueryUseCase productDetailQueryUseCase;
    private final ProductSearchUseCase productSearchUseCase;

    @Operation(summary = "상품 목록 검색")
    @GetMapping
    public ApiResponse<PageResponse<ProductSummaryResponse>> search(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false) Long brandId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        PageResult<ProductSummaryInfo> result = productSearchUseCase.search(keyword, categoryId, brandId, page, size);
        return ApiResponse.success(PageResponse.of(result, ProductSummaryResponse::from));
    }

    @Operation(summary = "상품 상세 조회")
    @GetMapping("/{productId}")
    public ApiResponse<ProductDetailResponse> getDetail(@PathVariable Long productId) {
        ProductDetailInfo info = productDetailQueryUseCase.getDetail(productId);
        return ApiResponse.success(ProductDetailResponse.from(info));
    }
}
