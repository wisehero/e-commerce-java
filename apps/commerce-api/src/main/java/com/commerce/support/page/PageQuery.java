package com.commerce.support.page;

import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

/**
 * 목록 조회의 페이징 입력. page/size 검증을 한 곳에 모아,
 * 검증되지 않은 값이 Spring의 PageRequest.of(...)에 닿아 IllegalArgumentException(→500)으로
 * 터지기 전에 BAD_REQUEST로 막는다. 모든 목록 조회가 이 규칙을 단일 진실 원천으로 공유한다.
 */
public record PageQuery(int page, int size) {

    private static final int MAX_SIZE = 100;

    public PageQuery {
        if (page < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "페이지 번호는 0 이상이어야 합니다.");
        }
        if (size <= 0 || size > MAX_SIZE) {
            throw new CoreException(ErrorType.BAD_REQUEST,
                String.format("페이지 크기는 1~%d 사이여야 합니다.", MAX_SIZE));
        }
    }
}
