package com.commerce.application.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
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

import com.commerce.application.product.ProductRegisterCommand.OptionValueCommand;
import com.commerce.application.product.ProductRegisterCommand.SkuRegisterCommand;
import com.commerce.domain.brand.Brand;
import com.commerce.domain.brand.BrandRepository;
import com.commerce.domain.brand.BrandStatus;
import com.commerce.domain.product.Product;
import com.commerce.domain.product.ProductRepository;
import com.commerce.domain.product.Sku;
import com.commerce.domain.product.SkuRepository;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

@ExtendWith(MockitoExtension.class)
class ProductRegisterUseCaseTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private SkuRepository skuRepository;

    @Mock
    private BrandRepository brandRepository;

    @InjectMocks
    private ProductRegisterUseCase useCase;

    private ProductRegisterCommand commandWithSkus(List<SkuRegisterCommand> skus) {
        return new ProductRegisterCommand("맨투맨", "기모", 1L, 2L, "img.jpg", skus);
    }

    private SkuRegisterCommand skuCommand(long price, int stock) {
        return new SkuRegisterCommand(List.of(new OptionValueCommand("색상", "빨강")), price, stock);
    }

    @Nested
    @DisplayName("상품 등록")
    class Register {

        @Test
        @DisplayName("유효한 명령이면 상품과 옵션을 저장하고 ProductDetailInfo를 반환한다")
        void should_returnDetailInfo_when_register() {
            // given
            ProductRegisterCommand command = commandWithSkus(List.of(skuCommand(10000, 100)));
            given(brandRepository.findById(2L))
                .willReturn(Optional.of(Brand.reconstitute(2L, "나이키", "logo.jpg", BrandStatus.ACTIVE)));
            given(productRepository.save(any(Product.class))).willAnswer(inv -> {
                Product p = inv.getArgument(0);
                return Product.reconstitute(10L, p.getName(), p.getDescription(),
                    p.getCategoryId(), p.getBrandId(), p.getImageUrl(), p.getStatus());
            });
            given(skuRepository.saveAll(anyList())).willAnswer(inv -> inv.getArgument(0));

            // when
            ProductDetailInfo info = useCase.register(command);

            // then
            assertThat(info)
                .satisfies(i -> assertThat(i.id()).isEqualTo(10L))
                .satisfies(i -> assertThat(i.name()).isEqualTo("맨투맨"))
                .satisfies(i -> assertThat(i.brandName()).isEqualTo("나이키"))
                .satisfies(i -> assertThat(i.status()).isEqualTo("ON_SALE"))
                .satisfies(i -> assertThat(i.skus()).hasSize(1));
        }

        @Test
        @DisplayName("저장된 상품 id가 모든 SKU의 productId로 전파된다")
        void should_propagateProductId_toSkus_when_register() {
            // given
            ProductRegisterCommand command = commandWithSkus(List.of(skuCommand(10000, 100), skuCommand(20000, 50)));
            given(brandRepository.findById(2L))
                .willReturn(Optional.of(Brand.reconstitute(2L, "나이키", "logo.jpg", BrandStatus.ACTIVE)));
            given(productRepository.save(any(Product.class))).willAnswer(inv -> {
                Product p = inv.getArgument(0);
                return Product.reconstitute(10L, p.getName(), p.getDescription(),
                    p.getCategoryId(), p.getBrandId(), p.getImageUrl(), p.getStatus());
            });
            given(skuRepository.saveAll(anyList())).willAnswer(inv -> inv.getArgument(0));

            // when
            useCase.register(command);

            // then
            ArgumentCaptor<List<Sku>> captor = ArgumentCaptor.forClass(List.class);
            then(skuRepository).should().saveAll(captor.capture());
            assertThat(captor.getValue())
                .hasSize(2)
                .allSatisfy(sku -> assertThat(sku.getProductId()).isEqualTo(10L));
        }

        @Test
        @DisplayName("브랜드가 존재하지 않으면 BAD_REQUEST 예외가 발생하고 상품을 저장하지 않는다")
        void should_throwBadRequest_when_brandMissing() {
            // given
            ProductRegisterCommand command = commandWithSkus(List.of(skuCommand(10000, 100)));
            given(brandRepository.findById(2L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> useCase.register(command))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
            then(productRepository).should(never()).save(any());
            then(skuRepository).should(never()).saveAll(anyList());
        }

        @Test
        @DisplayName("옵션이 비어 있으면 BAD_REQUEST 예외가 발생하고 아무것도 저장하지 않는다")
        void should_throwBadRequest_when_skusEmpty() {
            // given
            ProductRegisterCommand command = commandWithSkus(List.of());

            // when & then
            assertThatThrownBy(() -> useCase.register(command))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
            then(productRepository).should(never()).save(any());
            then(skuRepository).should(never()).saveAll(anyList());
            then(brandRepository).should(never()).findById(any());
        }

        @Test
        @DisplayName("옵션 목록이 null이면 BAD_REQUEST 예외가 발생한다")
        void should_throwBadRequest_when_skusNull() {
            // given
            ProductRegisterCommand command = commandWithSkus(null);

            // when & then
            assertThatThrownBy(() -> useCase.register(command))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }
    }
}
