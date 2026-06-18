package com.commerce.domain.category;

import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

import lombok.Getter;

@Getter
public class Category {

    private static final int NAME_MAX = 50;
    private static final int ROOT_DEPTH = 1;
    private static final int MAX_DEPTH = 3;

    private Long id;
    private String name;
    private Long parentId;
    private int depth;
    private int sortOrder;
    private CategoryStatus status;

    private Category(Long id, String name, Long parentId, int depth, int sortOrder, CategoryStatus status) {
        this.id = id;
        this.name = name;
        this.parentId = parentId;
        this.depth = depth;
        this.sortOrder = sortOrder;
        this.status = status;
        validate();
    }

    public static Category registerRoot(String name, int sortOrder) {
        return new Category(null, name, null, ROOT_DEPTH, sortOrder, CategoryStatus.ACTIVE);
    }

    public static Category registerChild(String name, Long parentId, int parentDepth, int sortOrder) {
        if (parentId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "하위 카테고리는 부모가 필요합니다.");
        }
        return new Category(null, name, parentId, parentDepth + 1, sortOrder, CategoryStatus.ACTIVE);
    }

    public static Category reconstitute(Long id, String name, Long parentId, int depth, int sortOrder,
        CategoryStatus status) {
        return new Category(id, name, parentId, depth, sortOrder, status);
    }

    private void validate() {
        validateName(name);
        if (depth < ROOT_DEPTH || depth > MAX_DEPTH) {
            throw new CoreException(ErrorType.BAD_REQUEST,
                String.format("카테고리는 최대 %d단계까지만 생성할 수 있습니다.", MAX_DEPTH));
        }
        if (parentId == null && depth != ROOT_DEPTH) {
            throw new CoreException(ErrorType.BAD_REQUEST, "루트 카테고리는 부모가 없어야 합니다.");
        }
        if (parentId != null && depth == ROOT_DEPTH) {
            throw new CoreException(ErrorType.BAD_REQUEST, "하위 카테고리는 부모가 필요합니다.");
        }
        if (status == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "카테고리 상태는 필수입니다.");
        }
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank() || name.length() > NAME_MAX) {
            throw new CoreException(ErrorType.BAD_REQUEST,
                String.format("카테고리명은 1~%d자여야 합니다.", NAME_MAX));
        }
    }

    public boolean isVisible() {
        return status == CategoryStatus.ACTIVE;
    }

    public boolean isLeaf() {
        return depth == MAX_DEPTH;
    }

    public void rename(String newName) {
        validateName(newName);
        this.name = newName;
    }

    public void changeSortOrder(int newSortOrder) {
        this.sortOrder = newSortOrder;
    }

    public void activate() {
        if (status == CategoryStatus.ACTIVE) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이미 활성화된 카테고리입니다.");
        }
        this.status = CategoryStatus.ACTIVE;
    }

    public void deactivate() {
        if (status == CategoryStatus.INACTIVE) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이미 비활성화된 카테고리입니다.");
        }
        this.status = CategoryStatus.INACTIVE;
    }
}
