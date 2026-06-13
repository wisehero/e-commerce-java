package com.commerce.application.cart;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.commerce.domain.cart.Cart;
import com.commerce.domain.cart.CartRepository;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

import lombok.RequiredArgsConstructor;

/**
 * 장바구니 라인 제거. 없는 SKU 제거는 멱등(no-op)하게 동작한다.
 */
@Service
@RequiredArgsConstructor
public class CartRemoveItemUseCase {

    private final CartRepository cartRepository;
    private final CartInfoAssembler assembler;

    @Transactional
    public CartInfo removeItem(Long memberId, Long skuId) {
        Cart cart = cartRepository.findByMemberId(memberId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "장바구니가 비어 있습니다."));

        cart.removeItem(skuId);
        Cart saved = cartRepository.save(cart);
        return assembler.assemble(saved);
    }
}
