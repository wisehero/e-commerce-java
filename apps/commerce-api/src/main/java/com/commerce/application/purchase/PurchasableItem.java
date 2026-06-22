package com.commerce.application.purchase;

import com.commerce.domain.brand.Brand;
import com.commerce.domain.product.Product;
import com.commerce.domain.product.Sku;

/**
 * 구매 가능성 검증을 통과한 상품 항목. SKU·상품·브랜드를 함께 들고 있어
 * 장바구니·주문이 추가 조회 없이 스냅샷을 구성할 수 있다.
 */
public record PurchasableItem(Sku sku, Product product, Brand brand) {
}
