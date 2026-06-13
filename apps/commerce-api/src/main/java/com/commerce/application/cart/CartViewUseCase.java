package com.commerce.application.cart;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.commerce.domain.cart.Cart;
import com.commerce.domain.cart.CartRepository;

import lombok.RequiredArgsConstructor;

/**
 * 장바구니 조회. 카트가 없는 회원은 404가 아니라 빈 카트 뷰를 반환한다.
 * 라인은 Product/Sku를 live 조회해 enrich하고 상태 플래그를 단다(CartInfoAssembler).
 */
@Service
@RequiredArgsConstructor
public class CartViewUseCase {

    private final CartRepository cartRepository;
    private final CartInfoAssembler assembler;

    @Transactional(readOnly = true)
    public CartInfo view(Long memberId) {
        Cart cart = cartRepository.findByMemberId(memberId)
            .orElseGet(() -> Cart.create(memberId));
        return assembler.assemble(cart);
    }
}
