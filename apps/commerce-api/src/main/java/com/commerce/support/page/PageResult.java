package com.commerce.support.page;

import java.util.List;

public record PageResult<T>(
    List<T> items, long totalCount, int page, int size
) {

    public PageResult {
        items = (items == null) ? List.of() : List.copyOf(items);
    }

    public int totalPages() {
        return size <= 0 ? 0 : (int)Math.ceil((double)totalCount / size);
    }

    public boolean hasNext() {
        return (long)(page + 1) * size < totalCount;
    }
}
