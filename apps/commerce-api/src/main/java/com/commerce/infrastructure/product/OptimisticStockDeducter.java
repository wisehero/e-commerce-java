package com.commerce.infrastructure.product;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import com.commerce.domain.product.Sku;
import com.commerce.domain.product.StockDeducter;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

import lombok.RequiredArgsConstructor;

/**
 * 낙관적 락 전략. 로드→도메인 차감→saveAndFlush로 @Version 충돌을 동기 감지한다.
 * 충돌 시 재시도 없이 fail-fast(CONFLICT) — 세 전략을 같은 트랜잭션 조건으로 비교하기 위함.
 */
@Component("optimistic")
@RequiredArgsConstructor
public class OptimisticStockDeducter implements StockDeducter {

    private final SkuJpaRepository skuJpaRepository;

    @Override
    public void deduct(Long skuId, int quantity) {
        SkuJpaEntity entity = skuJpaRepository.findById(skuId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 SKU입니다: " + skuId));

        Sku sku = entity.toDomain();
        sku.decreaseStock(quantity);          // 도메인 모델이 재고 부족(BAD_REQUEST) 검증
        entity.updateFromDomain(sku);

        try {
            skuJpaRepository.saveAndFlush(entity);   // @Version 충돌을 여기서 감지
        } catch (OptimisticLockingFailureException e) {
            throw new CoreException(ErrorType.CONFLICT, "재고 변경이 충돌했습니다. 다시 시도해주세요.");
        }
    }
}
