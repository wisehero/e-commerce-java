package com.commerce.application.product;

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

import com.commerce.domain.product.Product;
import com.commerce.domain.product.ProductRepository;
import com.commerce.domain.product.ProductStatus;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

@ExtendWith(MockitoExtension.class)
class ProductStatusChangeUseCaseTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductStatusChangeUseCase useCase;

    private Product productWith(ProductStatus status) {
        return Product.reconstitute(1L, "맨투맨", "기모", 1L, 2L, "img.jpg", status);
    }

    private ProductStatus savedStatus() {
        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        then(productRepository).should().save(captor.capture());
        return captor.getValue().getStatus();
    }

    @Nested
    @DisplayName("상태 변경")
    class ChangeStatus {

        @Test
        @DisplayName("판매중 상품을 일시중지하면 SUSPENDED로 저장한다")
        void should_suspend_when_found() {
            // given
            given(productRepository.findById(1L)).willReturn(Optional.of(productWith(ProductStatus.ON_SALE)));
            given(productRepository.save(any(Product.class))).willAnswer(inv -> inv.getArgument(0));

            // when
            useCase.suspend(1L);

            // then
            assertThat(savedStatus()).isEqualTo(ProductStatus.SUSPENDED);
        }

        @Test
        @DisplayName("일시중지 상품을 재개하면 ON_SALE로 저장한다")
        void should_resume_when_found() {
            // given
            given(productRepository.findById(1L)).willReturn(Optional.of(productWith(ProductStatus.SUSPENDED)));
            given(productRepository.save(any(Product.class))).willAnswer(inv -> inv.getArgument(0));

            // when
            useCase.resume(1L);

            // then
            assertThat(savedStatus()).isEqualTo(ProductStatus.ON_SALE);
        }

        @Test
        @DisplayName("상품을 단종하면 DISCONTINUED로 저장한다")
        void should_discontinue_when_found() {
            // given
            given(productRepository.findById(1L)).willReturn(Optional.of(productWith(ProductStatus.ON_SALE)));
            given(productRepository.save(any(Product.class))).willAnswer(inv -> inv.getArgument(0));

            // when
            useCase.discontinue(1L);

            // then
            assertThat(savedStatus()).isEqualTo(ProductStatus.DISCONTINUED);
        }

        @Test
        @DisplayName("상품이 없으면 NOT_FOUND 예외가 발생하고 저장하지 않는다")
        void should_throwNotFound_when_productMissing() {
            // given
            given(productRepository.findById(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> useCase.suspend(1L))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.NOT_FOUND);
            then(productRepository).should(never()).save(any());
        }
    }
}
