package com.commerce.application.cart;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.commerce.domain.cart.CartRepository;

import lombok.RequiredArgsConstructor;

/**
 * 장바구니 비우기(주문 완료 후 등). 카트가 없으면 아무것도 하지 않는다.
 */
@Service
@RequiredArgsConstructor
public class CartClearUseCase {

    private final CartRepository cartRepository;

    @Transactional
    public void clear(Long memberId) {
        cartRepository.findByMemberId(memberId).ifPresent(cart -> {
            cart.clear();
            cartRepository.save(cart);
        });
    }
}
