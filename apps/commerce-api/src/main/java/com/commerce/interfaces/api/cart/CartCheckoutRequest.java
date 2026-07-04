package com.commerce.interfaces.api.cart;

import com.commerce.application.cart.CartCheckoutCommand;

import jakarta.validation.constraints.NotBlank;

public record CartCheckoutRequest(
    @NotBlank(message = "재고 차감 방식(lockMode)은 필수입니다.")
    String lockMode,

    Long couponId
) {

    public CartCheckoutCommand toCommand(Long memberId) {
        return new CartCheckoutCommand(memberId, lockMode, couponId);
    }
}
