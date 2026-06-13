package com.commerce.infrastructure.product;

import org.springframework.stereotype.Component;

import com.commerce.domain.product.Sku;
import com.commerce.domain.product.StockDeducter;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

import lombok.RequiredArgsConstructor;

/**
 * 비관적 락 전략. SELECT ... FOR UPDATE로 차감 대상 행을 잠그고 차감한다.
 * 락은 트랜잭션 종료까지 유지되어 동시 차감을 직렬화한다(재시도 불필요).
 */
@Component("pessimistic")
@RequiredArgsConstructor
public class PessimisticStockDeducter implements StockDeducter {

    private final SkuJpaRepository skuJpaRepository;

    @Override
    public void deduct(Long skuId, int quantity) {
        SkuJpaEntity entity = skuJpaRepository.findByIdForUpdate(skuId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 SKU입니다: " + skuId));

        Sku sku = entity.toDomain();
        sku.decreaseStock(quantity);          // 도메인 모델이 재고 부족(BAD_REQUEST) 검증
        entity.updateFromDomain(sku);         // 더티체킹으로 트랜잭션 종료 시 flush
    }
}
