package com.commerce.application.cart;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.commerce.application.purchase.PurchasableItem;
import com.commerce.application.purchase.PurchasableItemResolver;
import com.commerce.domain.cart.Cart;
import com.commerce.domain.cart.CartRepository;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

import lombok.RequiredArgsConstructor;

/**
 * 장바구니 수량 변경(절대값). 담기와 동일하게 구매 가능성(상품 판매상태·브랜드 활성·재고)을 검증한다.
 * 장바구니에 없는 SKU면 Cart가 NOT_FOUND를 던진다(제거는 removeItem).
 * 구매 가능성 판단은 {@link PurchasableItemResolver}에 위임해 담기·주문 생성과 같은 기준을 쓴다.
 */
@Service
@RequiredArgsConstructor
public class CartChangeQuantityUseCase {

    private final CartRepository cartRepository;
    private final PurchasableItemResolver purchasableItemResolver;
    private final CartInfoAssembler assembler;

    @Transactional
    public CartInfo changeQuantity(CartChangeQuantityCommand command) {
        Cart cart = cartRepository.findByMemberIdForUpdate(command.memberId())
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "장바구니가 비어 있습니다."));

        PurchasableItem item = purchasableItemResolver.resolve(command.skuId());
        purchasableItemResolver.requireEnoughStock(item, command.quantity());

        cart.changeQuantity(command.skuId(), command.quantity());
        Cart saved = cartRepository.save(cart);
        return assembler.assemble(saved);
    }
}
