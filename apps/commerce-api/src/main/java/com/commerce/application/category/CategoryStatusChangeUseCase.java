package com.commerce.application.category;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.commerce.domain.category.Category;
import com.commerce.domain.category.CategoryRepository;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

import lombok.RequiredArgsConstructor;

/**
 * 카테고리의 노출 상태(ACTIVE/INACTIVE)를 전환하는 유스케이스.
 * 시즌 종료·임시 숨김 등으로 메뉴에서 내리거나 다시 올릴 때 쓴다.
 */
@Service
@RequiredArgsConstructor
public class CategoryStatusChangeUseCase {

    private final CategoryRepository categoryRepository;

    /**
     * 카테고리를 노출 상태로 되돌린다(숨겼던 메뉴를 다시 보이게).
     */
    @Transactional
    public void activate(Long categoryId) {
        Category category = findCategory(categoryId);
        category.activate();
        categoryRepository.save(category);
    }

    /**
     * 카테고리를 비노출 상태로 내린다(삭제 대신 메뉴에서 감추기).
     */
    @Transactional
    public void deactivate(Long categoryId) {
        Category category = findCategory(categoryId);
        category.deactivate();
        categoryRepository.save(category);
    }

    private Category findCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 카테고리입니다."));
    }
}
