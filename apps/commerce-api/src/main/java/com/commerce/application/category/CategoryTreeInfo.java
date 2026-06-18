package com.commerce.application.category;

import java.util.List;

import com.commerce.domain.category.Category;

public record CategoryTreeInfo(
    Long id,
    String name,
    Long parentId,
    int depth,
    int sortOrder,
    String status,
    List<CategoryTreeInfo> children
) {
    public static CategoryTreeInfo of(Category category, List<CategoryTreeInfo> children) {
        return new CategoryTreeInfo(
            category.getId(),
            category.getName(),
            category.getParentId(),
            category.getDepth(),
            category.getSortOrder(),
            category.getStatus().name(),
            children
        );
    }
}
