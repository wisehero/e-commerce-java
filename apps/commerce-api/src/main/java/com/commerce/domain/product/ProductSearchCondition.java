package com.commerce.domain.product;

import java.util.List;

import com.commerce.support.page.PageQuery;

public record ProductSearchCondition(
    String keyword, List<Long> categoryIds, Long brandId, int page, int size
) {

    public ProductSearchCondition {
        new PageQuery(page, size);
    }

}
