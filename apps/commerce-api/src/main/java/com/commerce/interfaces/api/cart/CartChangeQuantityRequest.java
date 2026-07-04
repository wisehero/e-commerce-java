package com.commerce.interfaces.api.cart;

import com.commerce.application.cart.CartChangeQuantityCommand;

import jakarta.validation.constraints.Min;
public record CartChangeQuantityRequest(
    @Min(value = 1, message = "수량은 1개 이상이어야 합니다.")
    int quantity
) {
    public CartChangeQuantityCommand toCommand(Long memberId, Long skuId) {
        return new CartChangeQuantityCommand(memberId, skuId, quantity);
    }
}
