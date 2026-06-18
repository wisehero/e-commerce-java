package com.commerce.application.category;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.commerce.domain.category.Category;
import com.commerce.domain.category.CategoryRepository;
import com.commerce.domain.category.CategoryStatus;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

@ExtendWith(MockitoExtension.class)
class CategoryDeleteUseCaseTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryDeleteUseCase useCase;

    @Test
    @DisplayName("존재하지 않는 카테고리면 NOT_FOUND 예외가 발생하고 삭제하지 않는다")
    void should_throwNotFound_when_categoryMissing() {
        // given
        given(categoryRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> useCase.delete(1L))
            .isInstanceOf(CoreException.class)
            .extracting("errorType").isEqualTo(ErrorType.NOT_FOUND);
        then(categoryRepository).should(never()).deleteById(any());
    }

    @Test
    @DisplayName("하위 카테고리가 있으면 CONFLICT 예외가 발생하고 삭제하지 않는다")
    void should_throwConflict_when_hasChildren() {
        // given
        given(categoryRepository.findById(10L))
            .willReturn(Optional.of(Category.reconstitute(10L, "패션", null, 1, 1, CategoryStatus.ACTIVE)));
        given(categoryRepository.existsByParentId(10L)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> useCase.delete(10L))
            .isInstanceOf(CoreException.class)
            .extracting("errorType").isEqualTo(ErrorType.CONFLICT);
        then(categoryRepository).should(never()).deleteById(any());
    }

    @Test
    @DisplayName("하위가 없으면 카테고리를 삭제한다")
    void should_delete_when_noChildren() {
        // given
        given(categoryRepository.findById(30L))
            .willReturn(Optional.of(Category.reconstitute(30L, "셔츠", 20L, 3, 0, CategoryStatus.ACTIVE)));
        given(categoryRepository.existsByParentId(30L)).willReturn(false);

        // when & then
        assertThatCode(() -> useCase.delete(30L)).doesNotThrowAnyException();
        then(categoryRepository).should().deleteById(30L);
    }
}
