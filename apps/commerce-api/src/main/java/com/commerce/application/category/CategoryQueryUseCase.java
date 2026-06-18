package com.commerce.application.category;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.commerce.domain.category.Category;
import com.commerce.domain.category.CategoryRepository;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

import lombok.RequiredArgsConstructor;

/**
 * 카테고리 조회 유스케이스.
 * 단건 상세와, 메뉴 렌더링에 쓰는 전체 트리(루트→리프 중첩) 두 가지를 제공한다.
 */
@Service
@RequiredArgsConstructor
public class CategoryQueryUseCase {

    private final CategoryRepository categoryRepository;

    /**
     * 카테고리 단건을 조회한다(편집 화면 등에서 노드 하나만 필요할 때).
     */
    @Transactional(readOnly = true)
    public CategoryInfo getCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 카테고리입니다."));
        return CategoryInfo.from(category);
    }

    /**
     * 전체 카테고리를 한 번에 읽어 루트부터 중첩 트리로 조립한다.
     * 3단계 고정이라 전체를 메모리에서 부모-자식으로 묶고 sortOrder로 정렬한다.
     */
    @Transactional(readOnly = true)
    public List<CategoryTreeInfo> getTree() {
        List<Category> all = categoryRepository.findAll();

        Map<Long, List<Category>> childrenByParentId = all.stream()
            .filter(c -> c.getParentId() != null)
            .collect(Collectors.groupingBy(Category::getParentId));

        return all.stream()
            .filter(c -> c.getParentId() == null)
            .sorted(Comparator.comparingInt(Category::getSortOrder))
            .map(root -> toTreeInfo(root, childrenByParentId))
            .toList();
    }

    private CategoryTreeInfo toTreeInfo(Category category, Map<Long, List<Category>> childrenByParentId) {
        List<CategoryTreeInfo> children = childrenByParentId.getOrDefault(category.getId(), List.of()).stream()
            .sorted(Comparator.comparingInt(Category::getSortOrder))
            .map(child -> toTreeInfo(child, childrenByParentId))
            .toList();
        return CategoryTreeInfo.of(category, children);
    }
}
