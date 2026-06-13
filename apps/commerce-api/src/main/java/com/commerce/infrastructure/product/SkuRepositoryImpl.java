package com.commerce.infrastructure.product;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.commerce.domain.product.Sku;
import com.commerce.domain.product.SkuRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SkuRepositoryImpl implements SkuRepository {

    private final SkuJpaRepository jpa;

    @Override
    public Sku save(Sku sku) {
        SkuJpaEntity saved;
        if (sku.getId() == null) {
            saved = jpa.save(SkuJpaEntity.fromDomain(sku));
        } else {
            SkuJpaEntity existing = jpa.findById(sku.getId())
                .orElseThrow(() -> new IllegalStateException("Sku not found: " + sku.getId()));
            existing.updateFromDomain(sku);
            saved = existing;
        }
        return saved.toDomain();
    }

    @Override
    public List<Sku> saveAll(List<Sku> skus) {
        List<SkuJpaEntity> entities = skus.stream()
            .map(SkuJpaEntity::fromDomain)
            .toList();
        return jpa.saveAll(entities).stream()
            .map(SkuJpaEntity::toDomain)
            .toList();
    }

    @Override
    public Optional<Sku> findById(Long id) {
        return jpa.findById(id).map(SkuJpaEntity::toDomain);
    }

    @Override
    public List<Sku> findByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();                       // IN () 쿼리 방지
        }
        return jpa.findAllById(ids).stream()
            .map(SkuJpaEntity::toDomain)
            .toList();
    }

    @Override
    public List<Sku> findByProductId(Long productId) {
        return jpa.findByProductId(productId).stream()
            .map(SkuJpaEntity::toDomain)
            .toList();
    }

    @Override
    public List<Sku> findByProductIds(List<Long> productIds) {
        if (productIds.isEmpty()) {
            return List.of();                       // IN () 쿼리 방지
        }
        return jpa.findByProductIdIn(productIds).stream()
            .map(SkuJpaEntity::toDomain)
            .toList();
    }
}
