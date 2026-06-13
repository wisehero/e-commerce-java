package com.commerce.application.product;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.commerce.domain.product.Product;
import com.commerce.domain.product.ProductRepository;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

import lombok.RequiredArgsConstructor;

/**
 * 상품의 판매 상태를 전이시키는 유스케이스.
 * 판매 일시중단·재개·영구 단종을 관리자 관점에서 처리한다.
 * 각 전이의 유효성(예: 단종된 상품은 재개 불가)은 Product 도메인이 강제하고,
 * 여기서는 대상 상품을 찾아 전이를 위임하고 영속화하는 오케스트레이션만 담당한다.
 */
@Service
@RequiredArgsConstructor
public class ProductStatusChangeUseCase {

    private final ProductRepository productRepository;

    /**
     * 판매를 일시 중단한다.
     * 가격 점검·정보 수정 등 <b>일시적</b> 사유로 노출을 내리되, 언제든 재개할 수 있는 상태로 둔다.
     */
    @Transactional
    public void suspend(Long productId) {
        Product product = findProduct(productId);

        product.suspend();

        productRepository.save(product);
    }

    /**
     * 일시 중단했던 판매를 재개한다.
     * 중단 사유가 해소되어 다시 노출·구매 가능한 상태로 되돌린다.
     */
    @Transactional
    public void resume(Long productId) {
        Product product = findProduct(productId);

        product.resume();

        productRepository.save(product);
    }

    /**
     * 상품을 영구 단종한다.
     * 더 이상 판매하지 않으며 되돌릴 수 없다.
     * 과거 주문 이력 보존을 위해 레코드는 남기고 노출에서만 제거한다(soft delete 대체).
     */
    @Transactional
    public void discontinue(Long productId) {
        Product product = findProduct(productId);

        product.discontinue();

        productRepository.save(product);
    }

    private Product findProduct(Long productId) {
        return productRepository.findById(productId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 상품입니다."));
    }
}
