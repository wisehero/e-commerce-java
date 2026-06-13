package com.commerce.application.cart;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.commerce.domain.brand.Brand;
import com.commerce.domain.brand.BrandRepository;
import com.commerce.domain.cart.Cart;
import com.commerce.domain.cart.CartRepository;
import com.commerce.domain.member.MemberRepository;
import com.commerce.domain.product.Product;
import com.commerce.domain.product.ProductRepository;
import com.commerce.domain.product.Sku;
import com.commerce.domain.product.SkuRepository;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

import lombok.RequiredArgsConstructor;

/**
 * 장바구니 담기. 회원·SKU·상품 판매상태·재고를 검증한 뒤 담는다(쓰레기 라인 차단).
 * 같은 SKU면 수량을 합산하며, 합산 결과 수량을 재고와 비교한다.
 */
@Service
@RequiredArgsConstructor
public class CartAddItemUseCase {

    private final CartRepository cartRepository;
    private final MemberRepository memberRepository;
    private final SkuRepository skuRepository;
    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final CartInfoAssembler assembler;

    @Transactional
    public CartInfo addItem(CartAddItemCommand command) {
        memberRepository.findById(command.memberId())
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 회원입니다."));

        Sku sku = skuRepository.findById(command.skuId())
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 상품 옵션입니다."));
        Product product = productRepository.findById(sku.getProductId())
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 상품입니다."));
        if (!product.isVisible()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "판매 중인 상품이 아닙니다.");
        }
        Brand brand = brandRepository.findById(product.getBrandId())
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 브랜드입니다."));
        if (!brand.isVisible()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "구매할 수 없는 브랜드입니다.");
        }

        Cart cart = cartRepository.findByMemberId(command.memberId())
            .orElseGet(() -> Cart.create(command.memberId()));

        int desiredQuantity = cart.quantityOf(command.skuId()) + command.quantity();
        if (sku.getStock().quantity() < desiredQuantity) {
            throw new CoreException(ErrorType.BAD_REQUEST, "재고가 부족합니다.");
        }

        cart.addItem(command.skuId(), command.quantity());
        Cart saved = cartRepository.save(cart);
        return assembler.assemble(saved);
    }
}
