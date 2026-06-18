package com.commerce.application.category;

public record CategoryRegisterCommand(
    String name,
    Long parentId,
    int sortOrder
) {
}
