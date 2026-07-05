package com.commerce.application.category;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.commerce.domain.category.CategoryRepository;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

import lombok.RequiredArgsConstructor;

/**
 * 카테고리를 물리 삭제하는 유스케이스.
 * 잘못 만든 노드를 정리하는 용도이며, 트리가 끊기지 않도록 하위가 있으면 삭제를 거부한다.
 */
@Service
@RequiredArgsConstructor
public class CategoryDeleteUseCase {

    private final CategoryRepository categoryRepository;

    /**
     * 카테고리를 삭제한다.
     * 하위 카테고리가 매달려 있으면 고아 노드가 생기므로 삭제를 막는다.
     * (매달린 상품 검증은 아직 하지 않는다.)
     */
    @Transactional
    public void delete(Long categoryId) {
        if (categoryRepository.findById(categoryId).isEmpty()) {
            throw new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 카테고리입니다.");
        }
        if (categoryRepository.existsByParentId(categoryId)) {
            throw new CoreException(ErrorType.CONFLICT, "하위 카테고리가 있어 삭제할 수 없습니다.");
        }
        categoryRepository.deleteById(categoryId);
    }
}
