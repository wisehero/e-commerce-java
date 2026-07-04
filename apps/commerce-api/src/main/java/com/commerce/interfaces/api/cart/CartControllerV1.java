package com.commerce.interfaces.api.cart;

import io.swagger.v3.oas.annotations.Operation;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.commerce.application.cart.CartAddItemUseCase;
import com.commerce.application.cart.CartChangeQuantityUseCase;
import com.commerce.application.cart.CartCheckoutUseCase;
import com.commerce.application.cart.CartClearUseCase;
import com.commerce.application.cart.CartRemoveItemUseCase;
import com.commerce.application.cart.CartViewUseCase;
import com.commerce.interfaces.api.ApiResponse;
import com.commerce.interfaces.api.security.AuthenticatedMember;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/carts")
@RequiredArgsConstructor
public class CartControllerV1 {

    private final CartViewUseCase cartViewUseCase;
    private final CartAddItemUseCase cartAddItemUseCase;
    private final CartChangeQuantityUseCase cartChangeQuantityUseCase;
    private final CartRemoveItemUseCase cartRemoveItemUseCase;
    private final CartClearUseCase cartClearUseCase;
    private final CartCheckoutUseCase cartCheckoutUseCase;

    @Operation(summary = "장바구니 조회")
    @GetMapping
    public ApiResponse<CartResponse> view(AuthenticatedMember authenticatedMember) {
        return ApiResponse.success(CartResponse.from(cartViewUseCase.view(authenticatedMember.memberId())));
    }

    @Operation(summary = "장바구니 상품 추가")
    @PostMapping("/items")
    public ApiResponse<CartResponse> addItem(AuthenticatedMember authenticatedMember,
        @Valid @RequestBody CartAddItemRequest request) {
        return ApiResponse.success(CartResponse.from(cartAddItemUseCase.addItem(
            request.toCommand(authenticatedMember.memberId()))));
    }

    @Operation(summary = "장바구니 체크아웃")
    @PostMapping("/checkout")
    public ApiResponse<CartCheckoutResponse> checkout(AuthenticatedMember authenticatedMember,
        @Valid @RequestBody CartCheckoutRequest request) {
        return ApiResponse.success(CartCheckoutResponse.from(cartCheckoutUseCase.checkout(
            request.toCommand(authenticatedMember.memberId()))));
    }

    @Operation(summary = "장바구니 상품 수량 변경")
    @PatchMapping("/items/{skuId}")
    public ApiResponse<CartResponse> changeQuantity(
        AuthenticatedMember authenticatedMember,
        @PathVariable Long skuId,
        @Valid @RequestBody CartChangeQuantityRequest request
    ) {
        return ApiResponse.success(CartResponse.from(cartChangeQuantityUseCase.changeQuantity(
            request.toCommand(authenticatedMember.memberId(), skuId))));
    }

    @Operation(summary = "장바구니 상품 삭제")
    @DeleteMapping("/items/{skuId}")
    public ApiResponse<CartResponse> removeItem(AuthenticatedMember authenticatedMember, @PathVariable Long skuId) {
        return ApiResponse.success(CartResponse.from(cartRemoveItemUseCase.removeItem(authenticatedMember.memberId(),
            skuId)));
    }

    @Operation(summary = "장바구니 비우기")
    @DeleteMapping
    public ApiResponse<Object> clear(AuthenticatedMember authenticatedMember) {
        cartClearUseCase.clear(authenticatedMember.memberId());
        return ApiResponse.success();
    }
}
