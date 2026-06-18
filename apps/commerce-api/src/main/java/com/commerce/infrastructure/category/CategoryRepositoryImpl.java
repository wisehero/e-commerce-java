package com.commerce.infrastructure.category;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.commerce.domain.category.Category;
import com.commerce.domain.category.CategoryRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class CategoryRepositoryImpl implements CategoryRepository {

    private final CategoryJpaRepository jpa;

    @Override
    public Category save(Category category) {
        CategoryJpaEntity saved;
        if (category.getId() == null) {
            saved = jpa.save(CategoryJpaEntity.fromDomain(category));
        } else {
            CategoryJpaEntity existing = jpa.findById(category.getId())
                .orElseThrow(() -> new IllegalStateException("Category not found: " + category.getId()));
            existing.updateFromDomain(category);
            saved = existing;
        }
        return saved.toDomain();
    }

    @Override
    public Optional<Category> findById(Long id) {
        return jpa.findById(id).map(CategoryJpaEntity::toDomain);
    }

    @Override
    public List<Category> findAll() {
        return jpa.findAll().stream()
            .map(CategoryJpaEntity::toDomain)
            .toList();
    }

    @Override
    public boolean existsByParentIdAndName(Long parentId, String name) {
        if (parentId == null) {
            return jpa.existsByParentIdIsNullAndName(name);
        }
        return jpa.existsByParentIdAndName(parentId, name);
    }

    @Override
    public boolean existsByParentId(Long parentId) {
        return jpa.existsByParentId(parentId);
    }

    @Override
    public List<Long> findSelfAndDescendantIds(Long categoryId) {
        // 트리는 최대 3단계이므로 직속 자식과 손자까지만 펼치면 모든 하위가 모인다.
        List<Long> ids = new ArrayList<>();
        ids.add(categoryId);

        List<Long> children = jpa.findIdsByParentId(categoryId);
        if (!children.isEmpty()) {
            ids.addAll(children);
            ids.addAll(jpa.findIdsByParentIdIn(children));
        }
        return ids;
    }

    @Override
    public void deleteById(Long id) {
        jpa.deleteById(id);
    }
}
