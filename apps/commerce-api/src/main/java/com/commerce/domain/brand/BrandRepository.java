package com.commerce.domain.brand;

import java.util.Optional;

public interface BrandRepository {

    Brand save(Brand brand);

    Optional<Brand> findById(Long id);

    boolean existsById(Long id);

    boolean existsByName(String name);
}
