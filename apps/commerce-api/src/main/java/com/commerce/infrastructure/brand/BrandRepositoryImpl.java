package com.commerce.infrastructure.brand;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.commerce.domain.brand.Brand;
import com.commerce.domain.brand.BrandRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class BrandRepositoryImpl implements BrandRepository {

    private final BrandJpaRepository jpa;

    @Override
    public Brand save(Brand brand) {
        BrandJpaEntity saved;
        if (brand.getId() == null) {
            saved = jpa.save(BrandJpaEntity.fromDomain(brand));
        } else {
            BrandJpaEntity existing = jpa.findById(brand.getId())
                .orElseThrow(() -> new IllegalStateException("Brand not found: " + brand.getId()));
            existing.updateFromDomain(brand);
            saved = existing;
        }
        return saved.toDomain();
    }

    @Override
    public Optional<Brand> findById(Long id) {
        return jpa.findById(id).map(BrandJpaEntity::toDomain);
    }

    @Override
    public boolean existsById(Long id) {
        return jpa.existsById(id);
    }

    @Override
    public boolean existsByName(String name) {
        return jpa.existsByName(name);
    }
}
