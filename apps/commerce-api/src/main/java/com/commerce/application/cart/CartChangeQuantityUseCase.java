package com.commerce.application.cart;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.commerce.domain.brand.Brand;
import com.commerce.domain.brand.BrandRepository;
import com.commerce.domain.cart.Cart;
import com.commerce.domain.cart.CartRepository;
import com.commerce.domain.product.Product;
import com.commerce.domain.product.ProductRepository;
import com.commerce.domain.product.Sku;
import com.commerce.domain.product.SkuRepository;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

import lombok.RequiredArgsConstructor;

/**
 * 장바구니 수량 변경(절대값). 담기와 동일하게 상품 판매상태·재고를 검증한다.
 * 장바구니에 없는 SKU면 Cart가 NOT_FOUND를 던진다(제거는 removeItem).
 */
@Service
@RequiredArgsConstructor
public class CartChangeQuantityUseCase {

    private final CartRepository cartRepository;
    private final SkuRepository skuRepository;
    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final CartInfoAssembler assembler;

    @Transactional
    public CartInfo changeQuantity(CartChangeQuantityCommand command) {
        Cart cart = cartRepository.findByMemberId(command.memberId())
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "장바구니가 비어 있습니다."));

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
        if (!sku.hasEnoughStock(command.quantity())) {
            throw new CoreException(ErrorType.BAD_REQUEST, "재고가 부족합니다.");
        }

        cart.changeQuantity(command.skuId(), command.quantity());
        Cart saved = cartRepository.save(cart);
        return assembler.assemble(saved);
    }
}
