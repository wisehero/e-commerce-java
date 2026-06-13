package com.commerce.interfaces.api.cart;

import java.util.List;

import com.commerce.application.cart.CartInfo;
import com.commerce.application.cart.CartLineInfo;

public record CartResponse(
    Long memberId,
    List<CartLineResponse> lines,
    long cartTotal
) {

    public static CartResponse from(CartInfo info) {
        return new CartResponse(
            info.memberId(),
            info.lines().stream().map(CartLineResponse::from).toList(),
            info.cartTotal()
        );
    }

    public record CartLineResponse(
        Long skuId,
        int quantity,
        String productName,
        String optionSummary,
        long salePrice,
        long lineSubtotal,
        String status
    ) {
        public static CartLineResponse from(CartLineInfo info) {
            return new CartLineResponse(
                info.skuId(),
                info.quantity(),
                info.productName(),
                info.optionSummary(),
                info.salePrice(),
                info.lineSubtotal(),
                info.status().name()
            );
        }
    }
}
