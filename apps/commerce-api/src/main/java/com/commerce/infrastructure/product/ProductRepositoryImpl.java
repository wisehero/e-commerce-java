package com.commerce.infrastructure.product;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.commerce.domain.product.Product;
import com.commerce.domain.product.ProductRepository;
import com.commerce.domain.product.ProductSearchCondition;
import com.commerce.domain.product.ProductStatus;
import com.commerce.support.page.PageResult;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepository {

    private final ProductJpaRepository jpa;

    @Override
    public Product save(Product product) {
        ProductJpaEntity saved;
        if (product.getId() == null) {
            saved = jpa.save(ProductJpaEntity.fromDomain(product));
        } else {
            ProductJpaEntity existing = jpa.findById(product.getId())
                .orElseThrow(() -> new IllegalStateException("Product not found: " + product.getId()));
            existing.updateFromDomain(product);
            saved = existing;
        }
        return saved.toDomain();
    }

    @Override
    public Optional<Product> findById(Long id) {
        return jpa.findById(id).map(ProductJpaEntity::toDomain);
    }

    @Override
    public List<Product> findByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();                       // IN () 쿼리 방지
        }
        return jpa.findAllById(ids).stream()
            .map(ProductJpaEntity::toDomain)
            .toList();
    }

    @Override
    public PageResult<Product> search(ProductSearchCondition condition) {
        Pageable pageable = PageRequest.of(condition.page(), condition.size());

        Page<ProductJpaEntity> page = jpa.search(
            ProductStatus.ON_SALE, condition.keyword(), condition.categoryId(), pageable
        );

        List<Product> items = page.getContent().stream()
            .map(ProductJpaEntity::toDomain)
            .toList();

        return new PageResult<>(items, page.getTotalElements(), condition.page(), condition.size());
    }
}
