package com.commerce.interfaces.api.category;

import com.commerce.application.category.CategoryUpdateCommand;

import jakarta.validation.constraints.NotBlank;

public record CategoryUpdateRequest(
    @NotBlank(message = "카테고리명은 필수입니다.")
    String name,

    int sortOrder
) {
    public CategoryUpdateCommand toCommand(Long categoryId) {
        return new CategoryUpdateCommand(categoryId, name, sortOrder);
    }
}
