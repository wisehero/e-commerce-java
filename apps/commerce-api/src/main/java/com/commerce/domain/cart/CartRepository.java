package com.commerce.domain.cart;

import java.util.Optional;

/**
 * 장바구니 영속화 포트. Cart Aggregate 전체(carts + cart_lines)를 책임진다.
 * CartLine은 Cart를 통해서만 접근하므로 별도 Repository를 노출하지 않는다.
 */
public interface CartRepository {

    Cart save(Cart cart);

    Optional<Cart> findByMemberId(Long memberId);
}
