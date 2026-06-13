package com.commerce.domain.product;

import java.util.Optional;

import com.commerce.support.page.PageResult;

public interface ProductRepository {

    Product save(Product product);

    Optional<Product> findById(Long id);

    PageResult<Product> search(ProductSearchCondition condition);
}
