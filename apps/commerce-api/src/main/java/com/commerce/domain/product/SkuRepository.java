package com.commerce.domain.product;

import java.util.List;
import java.util.Optional;

public interface SkuRepository {
    Sku save(Sku sku);

    List<Sku> saveAll(List<Sku> skus);

    Optional<Sku> findById(Long id);

    /** SKU id 묶음으로 배치 조회 (장바구니 조회 등 N+1 회피용). */
    List<Sku> findByIds(List<Long> ids);

    List<Sku> findByProductId(Long productId);

    List<Sku> findByProductIds(List<Long> productIds);
}
