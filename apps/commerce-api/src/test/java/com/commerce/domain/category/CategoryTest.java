package com.commerce.domain.category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

class CategoryTest {

    private static final String VALID_NAME = "패션";

    @Nested
    @DisplayName("registerRoot (루트 등록)")
    class RegisterRoot {

        @Test
        @DisplayName("id 없이 depth 1, 부모 없음, ACTIVE 상태로 생성된다")
        void should_createRoot_when_registerRoot() {
            // when
            Category root = Category.registerRoot(VALID_NAME, 1);

            // then
            assertThat(root)
                .satisfies(c -> assertThat(c.getId()).isNull())
                .satisfies(c -> assertThat(c.getName()).isEqualTo(VALID_NAME))
                .satisfies(c -> assertThat(c.getParentId()).isNull())
                .satisfies(c -> assertThat(c.getDepth()).isEqualTo(1))
                .satisfies(c -> assertThat(c.getStatus()).isEqualTo(CategoryStatus.ACTIVE));
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"", "   "})
        @DisplayName("이름이 공백이면 BAD_REQUEST 예외가 발생한다")
        void should_throwException_when_nameBlank(String name) {
            // when & then
            assertThatThrownBy(() -> Category.registerRoot(name, 1))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }

        @Test
        @DisplayName("이름이 50자를 초과하면 BAD_REQUEST 예외가 발생한다")
        void should_throwException_when_nameTooLong() {
            // given
            String tooLong = "가".repeat(51);

            // when & then
            assertThatThrownBy(() -> Category.registerRoot(tooLong, 1))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("registerChild (하위 등록)")
    class RegisterChild {

        @Test
        @DisplayName("부모 depth + 1로 생성되고 부모 ID를 보유한다")
        void should_createChild_when_registerChild() {
            // when
            Category child = Category.registerChild("상의", 10L, 1, 0);

            // then
            assertThat(child)
                .satisfies(c -> assertThat(c.getParentId()).isEqualTo(10L))
                .satisfies(c -> assertThat(c.getDepth()).isEqualTo(2))
                .satisfies(c -> assertThat(c.getStatus()).isEqualTo(CategoryStatus.ACTIVE));
        }

        @Test
        @DisplayName("부모가 최하위(depth 3)면 자식을 추가할 수 없어 BAD_REQUEST 예외가 발생한다")
        void should_throwException_when_parentIsLeaf() {
            // when & then
            assertThatThrownBy(() -> Category.registerChild("소분류", 10L, 3, 0))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }

        @Test
        @DisplayName("부모 ID가 null이면 BAD_REQUEST 예외가 발생한다")
        void should_throwException_when_parentIdNull() {
            // when & then
            assertThatThrownBy(() -> Category.registerChild("상의", null, 1, 0))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("reconstitute (영속 복원)")
    class Reconstitute {

        @Test
        @DisplayName("루트인데 depth가 1이 아니면 BAD_REQUEST 예외가 발생한다")
        void should_throwException_when_rootDepthMismatch() {
            // when & then
            assertThatThrownBy(() -> Category.reconstitute(1L, VALID_NAME, null, 2, 0, CategoryStatus.ACTIVE))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }

        @Test
        @DisplayName("부모가 있는데 depth가 1이면 BAD_REQUEST 예외가 발생한다")
        void should_throwException_when_childDepthIsRoot() {
            // when & then
            assertThatThrownBy(() -> Category.reconstitute(1L, VALID_NAME, 9L, 1, 0, CategoryStatus.ACTIVE))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }

        @Test
        @DisplayName("depth가 3을 초과하면 BAD_REQUEST 예외가 발생한다")
        void should_throwException_when_depthExceedsMax() {
            // when & then
            assertThatThrownBy(() -> Category.reconstitute(1L, VALID_NAME, 9L, 4, 0, CategoryStatus.ACTIVE))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("rename / changeSortOrder")
    class Modify {

        @Test
        @DisplayName("이름을 변경할 수 있다")
        void should_rename() {
            // given
            Category root = Category.registerRoot(VALID_NAME, 1);

            // when
            root.rename("가전");

            // then
            assertThat(root.getName()).isEqualTo("가전");
        }

        @Test
        @DisplayName("변경할 이름이 공백이면 BAD_REQUEST 예외가 발생한다")
        void should_throwException_when_renameBlank() {
            // given
            Category root = Category.registerRoot(VALID_NAME, 1);

            // when & then
            assertThatThrownBy(() -> root.rename("  "))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }

        @Test
        @DisplayName("정렬 순서를 변경할 수 있다")
        void should_changeSortOrder() {
            // given
            Category root = Category.registerRoot(VALID_NAME, 1);

            // when
            root.changeSortOrder(5);

            // then
            assertThat(root.getSortOrder()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("상태 전이")
    class StatusTransition {

        @Test
        @DisplayName("활성 상태면 비활성화할 수 있다")
        void should_deactivate_when_active() {
            // given
            Category root = Category.registerRoot(VALID_NAME, 1);

            // when
            root.deactivate();

            // then
            assertThat(root.getStatus()).isEqualTo(CategoryStatus.INACTIVE);
            assertThat(root.isVisible()).isFalse();
        }

        @Test
        @DisplayName("이미 활성 상태면 다시 활성화할 수 없다")
        void should_throwException_when_activate_alreadyActive() {
            // given
            Category root = Category.registerRoot(VALID_NAME, 1);

            // when & then
            assertThatThrownBy(root::activate)
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }

        @Test
        @DisplayName("비활성 상태면 다시 활성화할 수 있다")
        void should_activate_when_inactive() {
            // given
            Category root = Category.registerRoot(VALID_NAME, 1);
            root.deactivate();

            // when
            root.activate();

            // then
            assertThat(root.getStatus()).isEqualTo(CategoryStatus.ACTIVE);
            assertThat(root.isVisible()).isTrue();
        }

        @Test
        @DisplayName("이미 비활성 상태면 다시 비활성화할 수 없다")
        void should_throwException_when_deactivate_alreadyInactive() {
            // given
            Category root = Category.registerRoot(VALID_NAME, 1);
            root.deactivate();

            // when & then
            assertThatThrownBy(root::deactivate)
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }
    }
}
