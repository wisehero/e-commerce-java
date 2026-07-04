package com.commerce.interfaces.api.category;

import io.swagger.v3.oas.annotations.Operation;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.commerce.application.category.CategoryInfo;
import com.commerce.application.category.CategoryQueryUseCase;
import com.commerce.interfaces.api.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryControllerV1 {

    private final CategoryQueryUseCase categoryQueryUseCase;

    @Operation(summary = "카테고리 전체 트리 조회")
    @GetMapping
    public ApiResponse<List<CategoryTreeResponse>> getTree() {
        List<CategoryTreeResponse> tree = categoryQueryUseCase.getTree().stream()
            .map(CategoryTreeResponse::from)
            .toList();
        return ApiResponse.success(tree);
    }

    @Operation(summary = "카테고리 상세 조회")
    @GetMapping("/{categoryId}")
    public ApiResponse<CategoryResponse> getCategory(@PathVariable Long categoryId) {
        CategoryInfo info = categoryQueryUseCase.getCategory(categoryId);
        return ApiResponse.success(CategoryResponse.from(info));
    }
}
