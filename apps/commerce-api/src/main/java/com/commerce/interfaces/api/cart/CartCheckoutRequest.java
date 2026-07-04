package com.commerce.interfaces.api.cart;

import com.commerce.application.cart.CartCheckoutCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CartCheckoutRequest(
    @NotNull(message = "주문자는 필수입니다.")
    Long memberId,

    @NotBlank(message = "재고 차감 방식(lockMode)은 필수입니다.")
    String lockMode,

    Long couponId
) {

    public CartCheckoutCommand toCommand() {
        return new CartCheckoutCommand(memberId, lockMode, couponId);
    }
}
