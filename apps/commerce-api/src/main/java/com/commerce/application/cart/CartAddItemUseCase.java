package com.commerce.application.cart;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.commerce.application.purchase.PurchasableItem;
import com.commerce.application.purchase.PurchasableItemResolver;
import com.commerce.domain.cart.Cart;
import com.commerce.domain.cart.CartRepository;
import com.commerce.domain.member.MemberRepository;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

import lombok.RequiredArgsConstructor;

/**
 * 장바구니 담기. 회원과 구매 가능성(상품 판매상태·브랜드 활성·재고)을 검증한 뒤 담는다(쓰레기 라인 차단).
 * 같은 SKU면 수량을 합산하며, 합산 결과 수량을 재고와 비교한다.
 * 구매 가능성 판단은 {@link PurchasableItemResolver}에 위임해 주문 생성과 같은 기준을 쓴다.
 */
@Service
@RequiredArgsConstructor
public class CartAddItemUseCase {

    private final CartRepository cartRepository;
    private final MemberRepository memberRepository;
    private final PurchasableItemResolver purchasableItemResolver;
    private final CartInfoAssembler assembler;

    @Transactional
    public CartInfo addItem(CartAddItemCommand command) {
        memberRepository.findById(command.memberId())
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 회원입니다."));

        PurchasableItem item = purchasableItemResolver.resolve(command.skuId());

        Cart cart = cartRepository.findByMemberIdForUpdate(command.memberId())
            .orElseGet(() -> Cart.create(command.memberId()));

        int desiredQuantity = cart.quantityOf(command.skuId()) + command.quantity();
        purchasableItemResolver.requireEnoughStock(item, desiredQuantity);

        cart.addItem(command.skuId(), command.quantity());
        Cart saved = cartRepository.save(cart);
        return assembler.assemble(saved);
    }
}
