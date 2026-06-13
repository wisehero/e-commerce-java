package com.commerce.domain.product;

import java.util.List;
import java.util.Optional;

import com.commerce.support.page.PageResult;

public interface ProductRepository {

    Product save(Product product);

    Optional<Product> findById(Long id);

    /** Product id 묶음으로 배치 조회 (장바구니 조회 등 N+1 회피용). */
    List<Product> findByIds(List<Long> ids);

    PageResult<Product> search(ProductSearchCondition condition);
}
