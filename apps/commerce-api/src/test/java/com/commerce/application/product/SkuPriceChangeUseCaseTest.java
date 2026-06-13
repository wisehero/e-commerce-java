package com.commerce.application.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.commerce.domain.product.OptionValue;
import com.commerce.domain.product.Sku;
import com.commerce.domain.product.SkuRepository;
import com.commerce.domain.product.Stock;
import com.commerce.domain.shared.Money;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

@ExtendWith(MockitoExtension.class)
class SkuPriceChangeUseCaseTest {

    @Mock
    private SkuRepository skuRepository;

    @InjectMocks
    private SkuPriceChangeUseCase useCase;

    private Sku skuWith(long original, long sale) {
        return Sku.reconstitute(100L, 1L, List.of(new OptionValue("색상", "빨강")),
            new Money(original), new Money(sale), new Stock(100));
    }

    private Sku savedSku() {
        ArgumentCaptor<Sku> captor = ArgumentCaptor.forClass(Sku.class);
        then(skuRepository).should().save(captor.capture());
        return captor.getValue();
    }

    @Nested
    @DisplayName("할인 적용")
    class ApplyDiscount {

        @Test
        @DisplayName("SKU를 찾으면 할인가를 적용해 저장한다")
        void should_applyDiscount_when_found() {
            // given
            given(skuRepository.findById(100L)).willReturn(Optional.of(skuWith(10000, 10000)));
            given(skuRepository.save(any(Sku.class))).willAnswer(inv -> inv.getArgument(0));

            // when
            useCase.applyDiscount(new SkuApplyDiscountCommand(100L, 8000));

            // then
            assertThat(savedSku().getSalePrice()).isEqualTo(new Money(8000));
        }

        @Test
        @DisplayName("SKU가 없으면 NOT_FOUND 예외가 발생하고 저장하지 않는다")
        void should_throwNotFound_when_skuMissing() {
            // given
            given(skuRepository.findById(100L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> useCase.applyDiscount(new SkuApplyDiscountCommand(100L, 8000)))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.NOT_FOUND);
            then(skuRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("정가 변경")
    class ChangePrice {

        @Test
        @DisplayName("정가를 바꾸면 할인이 초기화되어 판매가도 새 정가로 저장한다")
        void should_changePrice_when_found() {
            // given (기존 할인 상태: 정가 10000, 판매가 8000)
            given(skuRepository.findById(100L)).willReturn(Optional.of(skuWith(10000, 8000)));
            given(skuRepository.save(any(Sku.class))).willAnswer(inv -> inv.getArgument(0));

            // when
            useCase.changePrice(new SkuChangePriceCommand(100L, 12000));

            // then
            assertThat(savedSku())
                .satisfies(s -> assertThat(s.getOriginalPrice()).isEqualTo(new Money(12000)))
                .satisfies(s -> assertThat(s.getSalePrice()).isEqualTo(new Money(12000)));
        }
    }
}
