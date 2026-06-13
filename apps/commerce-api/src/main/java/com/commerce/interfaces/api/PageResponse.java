package com.commerce.interfaces.api;

import java.util.List;
import java.util.function.Function;

import com.commerce.support.page.PageResult;

public record PageResponse<T>(
    List<T> items,
    long totalCount,
    int page,
    int size,
    int totalPages,
    boolean hasNext
) {

    public static <S, T> PageResponse<T> of(PageResult<S> result, Function<S, T> mapper) {
        return new PageResponse<>(
            result.items().stream().map(mapper).toList(),
            result.totalCount(),
            result.page(),
            result.size(),
            result.totalPages(),
            result.hasNext()
        );
    }
}
