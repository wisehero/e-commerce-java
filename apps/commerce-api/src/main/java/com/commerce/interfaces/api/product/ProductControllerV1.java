package com.commerce.interfaces.api.product;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.commerce.application.product.ProductDetailInfo;
import com.commerce.application.product.ProductDetailQueryUseCase;
import com.commerce.application.product.ProductRegisterUseCase;
import com.commerce.application.product.ProductSearchUseCase;
import com.commerce.application.product.ProductStatusChangeUseCase;
import com.commerce.application.product.ProductSummaryInfo;
import com.commerce.interfaces.api.ApiResponse;
import com.commerce.interfaces.api.PageResponse;
import com.commerce.support.page.PageResult;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductControllerV1 {

    private final ProductRegisterUseCase productRegisterUseCase;
    private final ProductDetailQueryUseCase productDetailQueryUseCase;
    private final ProductSearchUseCase productSearchUseCase;
    private final ProductStatusChangeUseCase productStatusChangeUseCase;

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

    @GetMapping("/{productId}")
    public ApiResponse<ProductDetailResponse> getDetail(@PathVariable Long productId) {
        ProductDetailInfo info = productDetailQueryUseCase.getDetail(productId);
        return ApiResponse.success(ProductDetailResponse.from(info));
    }

    @PostMapping
    public ApiResponse<ProductDetailResponse> register(@Valid @RequestBody ProductRegisterRequest request) {
        ProductDetailInfo info = productRegisterUseCase.register(request.toCommand());
        return ApiResponse.success(ProductDetailResponse.from(info));
    }

    @PostMapping("/{productId}/suspend")
    public ApiResponse<Object> suspend(@PathVariable Long productId) {
        productStatusChangeUseCase.suspend(productId);
        return ApiResponse.success();
    }

    @PostMapping("/{productId}/resume")
    public ApiResponse<Object> resume(@PathVariable Long productId) {
        productStatusChangeUseCase.resume(productId);
        return ApiResponse.success();
    }

    @PostMapping("/{productId}/discontinue")
    public ApiResponse<Object> discontinue(@PathVariable Long productId) {
        productStatusChangeUseCase.discontinue(productId);
        return ApiResponse.success();
    }
}
