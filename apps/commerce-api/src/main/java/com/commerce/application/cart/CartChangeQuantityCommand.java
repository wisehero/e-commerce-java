package com.commerce.application.cart;

public record CartChangeQuantityCommand(Long memberId, Long skuId, int quantity) {
}
