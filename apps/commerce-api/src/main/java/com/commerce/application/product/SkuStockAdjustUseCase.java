package com.commerce.application.product;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.commerce.domain.product.Sku;
import com.commerce.domain.product.SkuRepository;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

import lombok.RequiredArgsConstructor;

/**
 * SKU(판매 단위)의 판매 가능 재고를 늘리는 유스케이스.
 * 관리자 수동 입고·보정 경로이며, 향후 WMS 입고 이벤트 수신 시에도 이 진입점으로 재고를 채운다(§10 연동).
 * 주문에 의한 차감은 별도 책임 — 주문 도메인 단계에서 다룬다.
 */
@Service
@RequiredArgsConstructor
public class SkuStockAdjustUseCase {

    private final SkuRepository skuRepository;

    /**
     * SKU의 판매 가능 재고를 늘린다.
     * 입고·재고 보정 등으로 다시 팔 수 있는 수량을 확보하는 행위.
     */
    @Transactional
    public void restock(SkuRestockCommand command) {
        Sku sku = findSku(command.skuId());
        sku.restock(command.quantity());
        skuRepository.save(sku);
    }

    private Sku findSku(Long skuId) {
        return skuRepository.findById(skuId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 옵션(SKU)입니다."));
    }
}
