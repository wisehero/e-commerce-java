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
class SkuStockAdjustUseCaseTest {

    @Mock
    private SkuRepository skuRepository;

    @InjectMocks
    private SkuStockAdjustUseCase useCase;

    private Sku skuWithStock(int quantity) {
        return Sku.reconstitute(100L, 1L, List.of(new OptionValue("색상", "빨강")),
            new Money(10000), new Money(10000), new Stock(quantity));
    }

    @Nested
    @DisplayName("재고 입고")
    class Restock {

        @Test
        @DisplayName("SKU를 찾으면 재고를 늘려 저장한다")
        void should_restock_when_found() {
            // given
            given(skuRepository.findById(100L)).willReturn(Optional.of(skuWithStock(100)));
            given(skuRepository.save(any(Sku.class))).willAnswer(inv -> inv.getArgument(0));

            // when
            useCase.restock(new SkuRestockCommand(100L, 50));

            // then
            ArgumentCaptor<Sku> captor = ArgumentCaptor.forClass(Sku.class);
            then(skuRepository).should().save(captor.capture());
            assertThat(captor.getValue().getStock().quantity()).isEqualTo(150);
        }

        @Test
        @DisplayName("SKU가 없으면 NOT_FOUND 예외가 발생하고 저장하지 않는다")
        void should_throwNotFound_when_skuMissing() {
            // given
            given(skuRepository.findById(100L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> useCase.restock(new SkuRestockCommand(100L, 50)))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.NOT_FOUND);
            then(skuRepository).should(never()).save(any());
        }
    }
}
