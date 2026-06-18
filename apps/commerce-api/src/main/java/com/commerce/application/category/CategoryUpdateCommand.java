package com.commerce.application.category;

public record CategoryUpdateCommand(
    Long categoryId,
    String name,
    int sortOrder
) {
}
