package com.commerce.interfaces.api.cart;

import com.commerce.application.cart.CartAddItemCommand;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CartAddItemRequest(
    @NotNull(message = "SKU ID는 필수입니다.")
    Long skuId,

    @Min(value = 1, message = "수량은 1개 이상이어야 합니다.")
    int quantity
) {
    public CartAddItemCommand toCommand(Long memberId) {
        return new CartAddItemCommand(memberId, skuId, quantity);
    }
}
