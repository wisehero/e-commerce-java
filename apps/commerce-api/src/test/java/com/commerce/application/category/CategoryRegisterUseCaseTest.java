package com.commerce.application.category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.commerce.domain.category.Category;
import com.commerce.domain.category.CategoryRepository;
import com.commerce.domain.category.CategoryStatus;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

@ExtendWith(MockitoExtension.class)
class CategoryRegisterUseCaseTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryRegisterUseCase useCase;

    @Nested
    @DisplayName("루트 등록")
    class RegisterRoot {

        @Test
        @DisplayName("부모가 없으면 depth 1 루트로 저장하고 CategoryInfo를 반환한다")
        void should_saveRoot_when_parentIdNull() {
            // given
            CategoryRegisterCommand command = new CategoryRegisterCommand("패션", null, 1);
            given(categoryRepository.existsByParentIdAndName(null, "패션")).willReturn(false);
            given(categoryRepository.save(any(Category.class))).willAnswer(inv -> {
                Category c = inv.getArgument(0);
                return Category.reconstitute(1L, c.getName(), c.getParentId(), c.getDepth(), c.getSortOrder(),
                    c.getStatus());
            });

            // when
            CategoryInfo info = useCase.register(command);

            // then
            assertThat(info)
                .satisfies(i -> assertThat(i.id()).isEqualTo(1L))
                .satisfies(i -> assertThat(i.parentId()).isNull())
                .satisfies(i -> assertThat(i.depth()).isEqualTo(1))
                .satisfies(i -> assertThat(i.status()).isEqualTo("ACTIVE"));
        }

        @Test
        @DisplayName("다른 루트와 이름이 겹치면 CONFLICT 예외가 발생하고 저장하지 않는다")
        void should_throwConflict_when_rootNameDuplicated() {
            // given
            CategoryRegisterCommand command = new CategoryRegisterCommand("패션", null, 1);
            given(categoryRepository.existsByParentIdAndName(null, "패션")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> useCase.register(command))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.CONFLICT);
            then(categoryRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("하위 등록")
    class RegisterChild {

        @Test
        @DisplayName("부모 depth를 이어받아 depth+1로 저장한다")
        void should_saveChild_withParentDepthPlusOne() {
            // given
            CategoryRegisterCommand command = new CategoryRegisterCommand("상의", 10L, 0);
            given(categoryRepository.findById(10L))
                .willReturn(Optional.of(Category.reconstitute(10L, "패션", null, 1, 1, CategoryStatus.ACTIVE)));
            given(categoryRepository.existsByParentIdAndName(10L, "상의")).willReturn(false);
            given(categoryRepository.save(any(Category.class))).willAnswer(inv -> {
                Category c = inv.getArgument(0);
                return Category.reconstitute(11L, c.getName(), c.getParentId(), c.getDepth(), c.getSortOrder(),
                    c.getStatus());
            });

            // when
            useCase.register(command);

            // then
            ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
            then(categoryRepository).should().save(captor.capture());
            assertThat(captor.getValue())
                .satisfies(c -> assertThat(c.getParentId()).isEqualTo(10L))
                .satisfies(c -> assertThat(c.getDepth()).isEqualTo(2));
        }

        @Test
        @DisplayName("부모가 존재하지 않으면 BAD_REQUEST 예외가 발생하고 저장하지 않는다")
        void should_throwBadRequest_when_parentMissing() {
            // given
            CategoryRegisterCommand command = new CategoryRegisterCommand("상의", 10L, 0);
            given(categoryRepository.findById(10L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> useCase.register(command))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
            then(categoryRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("부모가 최하위(depth 3)면 BAD_REQUEST 예외가 발생한다")
        void should_throwBadRequest_when_parentIsLeaf() {
            // given
            CategoryRegisterCommand command = new CategoryRegisterCommand("소분류", 30L, 0);
            given(categoryRepository.findById(30L))
                .willReturn(Optional.of(Category.reconstitute(30L, "셔츠", 20L, 3, 0, CategoryStatus.ACTIVE)));
            given(categoryRepository.existsByParentIdAndName(30L, "소분류")).willReturn(false);

            // when & then
            assertThatThrownBy(() -> useCase.register(command))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
            then(categoryRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("같은 부모 아래 형제와 이름이 겹치면 CONFLICT 예외가 발생한다")
        void should_throwConflict_when_siblingNameDuplicated() {
            // given
            CategoryRegisterCommand command = new CategoryRegisterCommand("상의", 10L, 0);
            given(categoryRepository.findById(10L))
                .willReturn(Optional.of(Category.reconstitute(10L, "패션", null, 1, 1, CategoryStatus.ACTIVE)));
            given(categoryRepository.existsByParentIdAndName(10L, "상의")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> useCase.register(command))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.CONFLICT);
            then(categoryRepository).should(never()).save(any());
        }
    }
}
