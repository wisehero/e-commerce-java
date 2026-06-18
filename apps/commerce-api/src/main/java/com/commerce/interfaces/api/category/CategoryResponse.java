package com.commerce.interfaces.api.category;

import com.commerce.application.category.CategoryInfo;

public record CategoryResponse(
    Long id,
    String name,
    Long parentId,
    int depth,
    int sortOrder,
    String status
) {
    public static CategoryResponse from(CategoryInfo info) {
        return new CategoryResponse(
            info.id(),
            info.name(),
            info.parentId(),
            info.depth(),
            info.sortOrder(),
            info.status()
        );
    }
}
