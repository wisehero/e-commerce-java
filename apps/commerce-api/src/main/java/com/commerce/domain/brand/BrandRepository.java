package com.commerce.domain.brand;

import java.util.List;
import java.util.Optional;

public interface BrandRepository {

    Brand save(Brand brand);

    Optional<Brand> findById(Long id);

    List<Brand> findByIds(List<Long> ids);

    boolean existsById(Long id);

    boolean existsByName(String name);
}
