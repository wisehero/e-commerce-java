package com.commerce.infrastructure.cart;

import com.commerce.domain.cart.CartLine;
import com.commerce.infrastructure.jpa.BaseJpaEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 장바구니 라인 매핑. Cart Aggregate의 자식이며 cart_id로 루트를 ID 참조한다.
 * 스냅샷하지 않는다 — sku_id와 수량만 보유(가격·상품명은 조회 시 live).
 * (JPA 연관 어노테이션 미사용 — CartRepositoryImpl이 명시적으로 저장/조회)
 */
@Entity
@Table(name = "cart_lines", indexes = {
    @Index(name = "idx_cart_lines_cart_id", columnList = "cart_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CartLineJpaEntity extends BaseJpaEntity {

    @Column(name = "cart_id", nullable = false)
    private Long cartId;

    @Column(name = "sku_id", nullable = false)
    private Long skuId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    private CartLineJpaEntity(Long cartId, Long skuId, int quantity) {
        this.cartId = cartId;
        this.skuId = skuId;
        this.quantity = quantity;
    }

    public static CartLineJpaEntity fromDomain(Long cartId, CartLine line) {
        return new CartLineJpaEntity(cartId, line.getSkuId(), line.getQuantity());
    }

    public CartLine toDomain() {
        return CartLine.reconstitute(getId(), skuId, quantity);
    }
}
