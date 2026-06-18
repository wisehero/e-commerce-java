package com.commerce.infrastructure.category;

import com.commerce.domain.category.Category;
import com.commerce.domain.category.CategoryStatus;
import com.commerce.infrastructure.jpa.BaseJpaEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "categories",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_categories_parent_name", columnNames = {"parent_id", "name"})
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CategoryJpaEntity extends BaseJpaEntity {

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "depth", nullable = false)
    private int depth;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CategoryStatus status;

    private CategoryJpaEntity(String name, Long parentId, int depth, int sortOrder, CategoryStatus status) {
        this.name = name;
        this.parentId = parentId;
        this.depth = depth;
        this.sortOrder = sortOrder;
        this.status = status;
    }

    public static CategoryJpaEntity fromDomain(Category category) {
        return new CategoryJpaEntity(
            category.getName(),
            category.getParentId(),
            category.getDepth(),
            category.getSortOrder(),
            category.getStatus()
        );
    }

    public Category toDomain() {
        return Category.reconstitute(getId(), name, parentId, depth, sortOrder, status);
    }

    public void updateFromDomain(Category category) {
        this.name = category.getName();
        this.parentId = category.getParentId();
        this.depth = category.getDepth();
        this.sortOrder = category.getSortOrder();
        this.status = category.getStatus();
    }
}
