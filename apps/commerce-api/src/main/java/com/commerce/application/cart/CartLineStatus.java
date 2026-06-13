package com.commerce.application.cart;

/**
 * 장바구니 라인의 구매 가능성. Cart 도메인이 아니라 application이 조회 시점에
 * Product 상태 + Sku 재고로 파생한다(라인은 막지 않고 표시만).
 */
public enum CartLineStatus {
    /** 구매 가능: 상품 판매중 + 재고 ≥ 담은 수량. 카트 총액에 합산된다. */
    PURCHASABLE,
    /** 품절: 상품은 판매중이나 재고가 담은 수량보다 적음(0 포함). */
    OUT_OF_STOCK,
    /** 구매 불가: 상품이 판매중지·단종이거나 존재하지 않음. */
    UNAVAILABLE
}
