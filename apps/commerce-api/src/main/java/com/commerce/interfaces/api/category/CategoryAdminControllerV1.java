package com.commerce.interfaces.api.category;

import io.swagger.v3.oas.annotations.Operation;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.commerce.application.category.CategoryDeleteUseCase;
import com.commerce.application.category.CategoryInfo;
import com.commerce.application.category.CategoryRegisterUseCase;
import com.commerce.application.category.CategoryStatusChangeUseCase;
import com.commerce.application.category.CategoryUpdateUseCase;
import com.commerce.interfaces.api.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/categories")
@RequiredArgsConstructor
public class CategoryAdminControllerV1 {

    private final CategoryRegisterUseCase categoryRegisterUseCase;
    private final CategoryUpdateUseCase categoryUpdateUseCase;
    private final CategoryStatusChangeUseCase categoryStatusChangeUseCase;
    private final CategoryDeleteUseCase categoryDeleteUseCase;

    @Operation(summary = "카테고리 등록")
    @PostMapping
    public ApiResponse<CategoryResponse> register(@Valid @RequestBody CategoryRegisterRequest request) {
        CategoryInfo info = categoryRegisterUseCase.register(request.toCommand());
        return ApiResponse.success(CategoryResponse.from(info));
    }

    @Operation(summary = "카테고리 정보 수정")
    @PatchMapping("/{categoryId}")
    public ApiResponse<CategoryResponse> update(
        @PathVariable Long categoryId,
        @Valid @RequestBody CategoryUpdateRequest request
    ) {
        CategoryInfo info = categoryUpdateUseCase.update(request.toCommand(categoryId));
        return ApiResponse.success(CategoryResponse.from(info));
    }

    @Operation(summary = "카테고리 활성화")
    @PostMapping("/{categoryId}/activate")
    public ApiResponse<Object> activate(@PathVariable Long categoryId) {
        categoryStatusChangeUseCase.activate(categoryId);
        return ApiResponse.success();
    }

    @Operation(summary = "카테고리 비활성화")
    @PostMapping("/{categoryId}/deactivate")
    public ApiResponse<Object> deactivate(@PathVariable Long categoryId) {
        categoryStatusChangeUseCase.deactivate(categoryId);
        return ApiResponse.success();
    }

    @Operation(summary = "카테고리 삭제")
    @DeleteMapping("/{categoryId}")
    public ApiResponse<Object> delete(@PathVariable Long categoryId) {
        categoryDeleteUseCase.delete(categoryId);
        return ApiResponse.success();
    }
}
