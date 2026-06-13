package com.commerce.infrastructure.product;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SkuJpaRepository extends JpaRepository<SkuJpaEntity, Long> {

    List<SkuJpaEntity> findByProductId(Long productId);

    List<SkuJpaEntity> findByProductIdIn(List<Long> productIds);
}
