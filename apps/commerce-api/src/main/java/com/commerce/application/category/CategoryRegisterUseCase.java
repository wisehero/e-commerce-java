package com.commerce.application.category;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.commerce.domain.category.Category;
import com.commerce.domain.category.CategoryRepository;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

import lombok.RequiredArgsConstructor;

/**
 * 카테고리 트리에 새 노드를 추가하는 유스케이스.
 * 부모 유무로 루트/하위 생성을 가르고, 같은 부모 아래 이름 중복과 최대 깊이를 함께 막는다.
 */
@Service
@RequiredArgsConstructor
public class CategoryRegisterUseCase {

    private final CategoryRepository categoryRepository;

    /**
     * 카테고리를 등록한다.
     * parentId가 없으면 루트로, 있으면 부모 depth를 이어받아 하위로 생성한다.
     * 부모는 실재해야 하고(끊긴 트리 방지), 형제 간 이름 중복은 허용하지 않는다.
     * 깊이 상한(3단계) 위반은 도메인 모델이 스스로 막는다.
     */
    @Transactional
    public CategoryInfo register(CategoryRegisterCommand command) {
        String name = normalizeName(command.name());
        Long parentId = command.parentId();

        Category category;
        if (parentId == null) {
            // 루트 등록: 형제(다른 루트)와 이름이 겹치면 안 된다.
            if (categoryRepository.existsByParentIdAndName(null, name)) {
                throw new CoreException(ErrorType.CONFLICT, "이미 존재하는 카테고리명입니다.");
            }
            category = Category.registerRoot(name, command.sortOrder());
        } else {
            // 하위 등록: 부모가 실재해야 하고, 같은 부모 아래 형제와 이름이 겹치면 안 된다.
            Category parent = categoryRepository.findById(parentId)
                .orElseThrow(() -> new CoreException(ErrorType.BAD_REQUEST, "존재하지 않는 부모 카테고리입니다."));
            if (categoryRepository.existsByParentIdAndName(parentId, name)) {
                throw new CoreException(ErrorType.CONFLICT, "이미 존재하는 카테고리명입니다.");
            }
            category = Category.registerChild(name, parentId, parent.getDepth(), command.sortOrder());
        }

        return CategoryInfo.from(categoryRepository.save(category));
    }

    private String normalizeName(String name) {
        return name == null ? null : name.trim();
    }
}
