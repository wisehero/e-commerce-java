package com.commerce.application.cart;

/**
 * 장바구니 라인 조회 결과(application 파생). 저장값(skuId·quantity)에 더해
 * 조회 시점의 live 정보(상품명·옵션·판매가·소계)와 구매 가능성(status)을 담는다.
 */
public record CartLineInfo(
    Long skuId,
    int quantity,
    String productName,
    String optionSummary,
    long salePrice,
    long lineSubtotal,
    CartLineStatus status
) {
}
