package com.commerce.support.page;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

class PageQueryTest {

    @Test
    @DisplayName("page 0, size 1은 하한 경계값으로 허용된다")
    void should_allow_when_lowerBounds() {
        assertThatCode(() -> new PageQuery(0, 1)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("size 100은 상한 경계값으로 허용된다")
    void should_allow_when_maxSize() {
        PageQuery pageQuery = new PageQuery(0, 100);

        assertThat(pageQuery.size()).isEqualTo(100);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, -10})
    @DisplayName("음수 page는 BAD_REQUEST로 막는다")
    void should_throwBadRequest_when_negativePage(int page) {
        assertThatThrownBy(() -> new PageQuery(page, 10))
            .isInstanceOf(CoreException.class)
            .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 101})
    @DisplayName("size가 1~100 범위를 벗어나면 BAD_REQUEST로 막는다")
    void should_throwBadRequest_when_sizeOutOfRange(int size) {
        assertThatThrownBy(() -> new PageQuery(0, size))
            .isInstanceOf(CoreException.class)
            .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
    }
}
