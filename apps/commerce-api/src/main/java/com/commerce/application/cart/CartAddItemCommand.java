package com.commerce.application.cart;

public record CartAddItemCommand(Long memberId, Long skuId, int quantity) {
}
