package com.commerce.application.product;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.commerce.domain.product.Sku;
import com.commerce.domain.product.SkuRepository;
import com.commerce.domain.shared.Money;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

import lombok.RequiredArgsConstructor;

/**
 * SKU(판매 단위)의 가격을 변경하는 유스케이스.
 * 정가 대비 할인 적용과 정가 자체의 변경을 다룬다.
 * 가격 간 invariant(할인가 ≤ 정가)는 Sku 도메인이 강제하고, 여기서는 대상 조회·위임·영속화만 한다.
 */
@Service
@RequiredArgsConstructor
public class SkuPriceChangeUseCase {

    private final SkuRepository skuRepository;

    /**
     * SKU에 할인가를 적용한다.
     * 정가는 그대로 두고 실제 판매가(salePrice)만 낮춰, 고객에게 할인으로 노출되도록 한다.
     */
    @Transactional
    public void applyDiscount(SkuApplyDiscountCommand command) {
        Sku sku = findSku(command.skuId());
        sku.applyDiscount(new Money(command.salePrice()));
        skuRepository.save(sku);
    }

    /**
     * SKU의 정가를 변경한다.
     * 가격 정책 자체가 바뀌는 경우이므로, 기존 할인은 초기화되고 판매가도 새 정가를 따른다.
     */
    @Transactional
    public void changePrice(SkuChangePriceCommand command) {
        Sku sku = findSku(command.skuId());
        sku.changePrice(new Money(command.originalPrice()));
        skuRepository.save(sku);
    }

    private Sku findSku(Long skuId) {
        return skuRepository.findById(skuId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 옵션(SKU)입니다."));
    }
}
