package com.commerce.domain.category;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository {

    Category save(Category category);

    Optional<Category> findById(Long id);

    List<Category> findAll();

    boolean existsByParentIdAndName(Long parentId, String name);

    boolean existsByParentId(Long parentId);

    /** 주어진 카테고리와 그 모든 하위 카테고리의 id를 모은다(상위 카테고리로 검색 시 하위 상품까지 포함하기 위함). */
    List<Long> findSelfAndDescendantIds(Long categoryId);

    void deleteById(Long id);
}
