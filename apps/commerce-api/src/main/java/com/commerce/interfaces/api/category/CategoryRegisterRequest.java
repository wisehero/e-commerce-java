package com.commerce.interfaces.api.category;

import com.commerce.application.category.CategoryRegisterCommand;

import jakarta.validation.constraints.NotBlank;

public record CategoryRegisterRequest(
    @NotBlank(message = "카테고리명은 필수입니다.")
    String name,

    Long parentId,

    int sortOrder
) {
    public CategoryRegisterCommand toCommand() {
        return new CategoryRegisterCommand(name, parentId, sortOrder);
    }
}
