package com.commerce.domain.product;

import java.util.List;
import java.util.Optional;

public interface SkuRepository {
    Sku save(Sku sku);

    List<Sku> saveAll(List<Sku> skus);

    Optional<Sku> findById(Long id);

    List<Sku> findByProductId(Long productId);

    List<Sku> findByProductIds(List<Long> productIds);
}
