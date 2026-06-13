package com.commerce.interfaces.api.cart;

import com.commerce.application.cart.CartChangeQuantityCommand;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CartChangeQuantityRequest(
    @NotNull(message = "회원 ID는 필수입니다.")
    Long memberId,

    @Min(value = 1, message = "수량은 1개 이상이어야 합니다.")
    int quantity
) {
    public CartChangeQuantityCommand toCommand(Long skuId) {
        return new CartChangeQuantityCommand(memberId, skuId, quantity);
    }
}
