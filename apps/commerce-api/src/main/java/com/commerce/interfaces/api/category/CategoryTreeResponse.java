package com.commerce.interfaces.api.category;

import java.util.List;

import com.commerce.application.category.CategoryTreeInfo;

public record CategoryTreeResponse(
    Long id,
    String name,
    Long parentId,
    int depth,
    int sortOrder,
    String status,
    List<CategoryTreeResponse> children
) {
    public static CategoryTreeResponse from(CategoryTreeInfo info) {
        return new CategoryTreeResponse(
            info.id(),
            info.name(),
            info.parentId(),
            info.depth(),
            info.sortOrder(),
            info.status(),
            info.children().stream()
                .map(CategoryTreeResponse::from)
                .toList()
        );
    }
}
