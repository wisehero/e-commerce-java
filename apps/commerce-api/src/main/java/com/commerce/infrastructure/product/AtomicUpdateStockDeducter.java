package com.commerce.infrastructure.product;

import org.springframework.stereotype.Component;

import com.commerce.domain.product.StockDeducter;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

import lombok.RequiredArgsConstructor;

/**
 * 조건부 원자 UPDATE 전략. {@code UPDATE ... SET stock = stock - q WHERE id = ? AND stock >= q}
 * 한 방으로 차감해 경합에 강하다. 단 도메인 모델(Sku.decreaseStock)을 우회하는 비교용 전략이다.
 *
 * <p>참고: 이 전략은 @Version을 증가시키지 않는다(JPQL 벌크 UPDATE 특성). 한 요청은 한 전략만
 * 쓰므로 비교 목적엔 무방하나, 실제 운영에선 전략을 하나로 고정한다.
 */
@Component("atomic")
@RequiredArgsConstructor
public class AtomicUpdateStockDeducter implements StockDeducter {

    private final SkuJpaRepository skuJpaRepository;

    @Override
    public void deduct(Long skuId, int quantity) {
        int affected = skuJpaRepository.deductStock(skuId, quantity);
        if (affected == 0) {
            if (!skuJpaRepository.existsById(skuId)) {
                throw new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 SKU입니다: " + skuId);
            }
            throw new CoreException(ErrorType.BAD_REQUEST, "재고가 부족합니다.");
        }
    }
}
