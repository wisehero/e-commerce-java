package com.commerce.application.category;

import com.commerce.domain.category.Category;

public record CategoryInfo(
    Long id,
    String name,
    Long parentId,
    int depth,
    int sortOrder,
    String status
) {
    public static CategoryInfo from(Category category) {
        return new CategoryInfo(
            category.getId(),
            category.getName(),
            category.getParentId(),
            category.getDepth(),
            category.getSortOrder(),
            category.getStatus().name()
        );
    }
}
