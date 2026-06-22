package com.commerce.application.purchase;

import org.springframework.stereotype.Component;

import com.commerce.domain.brand.Brand;
import com.commerce.domain.brand.BrandRepository;
import com.commerce.domain.product.Product;
import com.commerce.domain.product.ProductRepository;
import com.commerce.domain.product.Sku;
import com.commerce.domain.product.SkuRepository;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

/**
 * 구매 가능성 정책. "이 SKU를 지금 살 수 있는가?"를 한 곳에서 판단한다.
 *
 * <p>SKU→상품→브랜드를 조회해 상품 판매 상태와 브랜드 활성 상태를 검증하고,
 * 필요하면 수량 기준 재고 충분 여부를 본다. 장바구니 담기·수량 변경과 주문 생성이
 * 같은 기준을 공유하도록 이 객체를 거친다.
 *
 * <p>조회·상태 검증(resolve)과 재고 검증(requireEnoughStock)을 나눈 이유: 장바구니는
 * "기존 담긴 수량 + 추가 수량"을 알아야 재고를 비교할 수 있어 Cart를 조회한 뒤에 재고를
 * 검증하고, 주문은 재고를 StockDeducter로 차감하므로 사전 재고 검증을 하지 않는다.
 */
@Component
public class PurchasableItemResolver {

    private final SkuRepository skuRepository;
    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;

    public PurchasableItemResolver(SkuRepository skuRepository, ProductRepository productRepository,
        BrandRepository brandRepository) {
        this.skuRepository = skuRepository;
        this.productRepository = productRepository;
        this.brandRepository = brandRepository;
    }

    /** SKU·상품·브랜드를 조회하고 판매/활성 상태를 검증한다. 재고는 보지 않는다. */
    public PurchasableItem resolve(Long skuId) {
        Sku sku = skuRepository.findById(skuId)
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
        return new PurchasableItem(sku, product, brand);
    }

    /** 요청 수량만큼 재고가 있는지 검증한다. 부족하면 BAD_REQUEST. */
    public void requireEnoughStock(PurchasableItem item, int requiredQuantity) {
        if (!item.sku().hasEnoughStock(requiredQuantity)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "재고가 부족합니다.");
        }
    }
}
