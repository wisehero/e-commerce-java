package com.commerce.application.category;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.commerce.domain.category.Category;
import com.commerce.domain.category.CategoryRepository;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

import lombok.RequiredArgsConstructor;

/**
 * 카테고리의 표시 속성(이름·정렬 순서)을 수정하는 유스케이스.
 * 부모를 바꾸는 이동은 다루지 않는다(트리 위치는 생성 시 고정).
 */
@Service
@RequiredArgsConstructor
public class CategoryUpdateUseCase {

    private final CategoryRepository categoryRepository;

    /**
     * 카테고리 이름과 정렬 순서를 변경한다.
     * 이름을 실제로 바꿀 때만 형제 중복을 재검증해(자기 이름과의 충돌 오판 방지),
     * 같은 부모 아래 다른 형제와 겹치면 막는다.
     */
    @Transactional
    public CategoryInfo update(CategoryUpdateCommand command) {
        Category category = categoryRepository.findById(command.categoryId())
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 카테고리입니다."));

        String name = normalizeName(command.name());
        if (!category.getName().equals(name)
            && categoryRepository.existsByParentIdAndName(category.getParentId(), name)) {
            throw new CoreException(ErrorType.CONFLICT, "이미 존재하는 카테고리명입니다.");
        }

        category.rename(name);
        category.changeSortOrder(command.sortOrder());

        return CategoryInfo.from(categoryRepository.save(category));
    }

    private String normalizeName(String name) {
        return name == null ? null : name.trim();
    }
}
