package com.commerce.interfaces.api.cart;

import io.swagger.v3.oas.annotations.Operation;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.commerce.application.cart.CartAddItemUseCase;
import com.commerce.application.cart.CartChangeQuantityUseCase;
import com.commerce.application.cart.CartClearUseCase;
import com.commerce.application.cart.CartRemoveItemUseCase;
import com.commerce.application.cart.CartViewUseCase;
import com.commerce.interfaces.api.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 장바구니 API. 소유자 식별은 memberId 파라미터/바디로 받는다.
 * TODO: 인증 도입 시 memberId는 security context에서 가져오고 본인 카트만 접근하도록 강제한다.
 */
@RestController
@RequestMapping("/api/v1/carts")
@RequiredArgsConstructor
public class CartControllerV1 {

    private final CartViewUseCase cartViewUseCase;
    private final CartAddItemUseCase cartAddItemUseCase;
    private final CartChangeQuantityUseCase cartChangeQuantityUseCase;
    private final CartRemoveItemUseCase cartRemoveItemUseCase;
    private final CartClearUseCase cartClearUseCase;

    @Operation(summary = "장바구니 조회")
    @GetMapping
    public ApiResponse<CartResponse> view(@RequestParam Long memberId) {
        return ApiResponse.success(CartResponse.from(cartViewUseCase.view(memberId)));
    }

    @Operation(summary = "장바구니 상품 추가")
    @PostMapping("/items")
    public ApiResponse<CartResponse> addItem(@Valid @RequestBody CartAddItemRequest request) {
        return ApiResponse.success(CartResponse.from(cartAddItemUseCase.addItem(request.toCommand())));
    }

    @Operation(summary = "장바구니 상품 수량 변경")
    @PatchMapping("/items/{skuId}")
    public ApiResponse<CartResponse> changeQuantity(
        @PathVariable Long skuId,
        @Valid @RequestBody CartChangeQuantityRequest request
    ) {
        return ApiResponse.success(CartResponse.from(cartChangeQuantityUseCase.changeQuantity(request.toCommand(skuId))));
    }

    @Operation(summary = "장바구니 상품 삭제")
    @DeleteMapping("/items/{skuId}")
    public ApiResponse<CartResponse> removeItem(@PathVariable Long skuId, @RequestParam Long memberId) {
        return ApiResponse.success(CartResponse.from(cartRemoveItemUseCase.removeItem(memberId, skuId)));
    }

    @Operation(summary = "장바구니 비우기")
    @DeleteMapping
    public ApiResponse<Object> clear(@RequestParam Long memberId) {
        cartClearUseCase.clear(memberId);
        return ApiResponse.success();
    }
}
