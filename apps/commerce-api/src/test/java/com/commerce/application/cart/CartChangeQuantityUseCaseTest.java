package com.commerce.application.cart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.commerce.application.purchase.PurchasableItemResolver;
import com.commerce.domain.brand.Brand;
import com.commerce.domain.brand.BrandRepository;
import com.commerce.domain.brand.BrandStatus;
import com.commerce.domain.cart.Cart;
import com.commerce.domain.cart.CartRepository;
import com.commerce.domain.product.OptionValue;
import com.commerce.domain.product.Product;
import com.commerce.domain.product.ProductRepository;
import com.commerce.domain.product.ProductStatus;
import com.commerce.domain.product.Sku;
import com.commerce.domain.product.SkuRepository;
import com.commerce.domain.product.Stock;
import com.commerce.domain.shared.Money;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

@ExtendWith(MockitoExtension.class)
class CartChangeQuantityUseCaseTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long SKU_ID = 10L;
    private static final Long PRODUCT_ID = 100L;

    @Mock
    private CartRepository cartRepository;
    @Mock
    private SkuRepository skuRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private BrandRepository brandRepository;
    @Mock
    private CartInfoAssembler assembler;

    private CartChangeQuantityUseCase useCase;

    @BeforeEach
    void setUp() {
        PurchasableItemResolver resolver = new PurchasableItemResolver(skuRepository, productRepository,
            brandRepository);
        useCase = new CartChangeQuantityUseCase(cartRepository, resolver, assembler);
    }

    private CartChangeQuantityCommand command(int quantity) {
        return new CartChangeQuantityCommand(MEMBER_ID, SKU_ID, quantity);
    }

    private Sku sku(int stock) {
        return Sku.reconstitute(SKU_ID, PRODUCT_ID, List.of(new OptionValue("색상", "빨강")),
            new Money(10000), new Money(8000), new Stock(stock));
    }

    private Product product(ProductStatus status) {
        return Product.reconstitute(PRODUCT_ID, "맨투맨", "설명", 1L, 2L, "img.jpg", status);
    }

    private Brand brand(BrandStatus status) {
        return Brand.reconstitute(2L, "나이키", "logo.jpg", status);
    }

    private Cart cartWithLine(int quantity) {
        Cart cart = Cart.create(MEMBER_ID);
        cart.addItem(SKU_ID, quantity);
        return cart;
    }

    @Test
    @DisplayName("카트가 없으면 NOT_FOUND 예외가 발생한다")
    void should_throwNotFound_when_cartMissing() {
        // given
        given(cartRepository.findByMemberIdForUpdate(MEMBER_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> useCase.changeQuantity(command(3)))
            .isInstanceOf(CoreException.class)
            .extracting("errorType").isEqualTo(ErrorType.NOT_FOUND);
    }

    @Test
    @DisplayName("재고보다 많은 수량으로 변경하면 BAD_REQUEST 예외가 발생한다")
    void should_throwBadRequest_when_exceedsStock() {
        // given — 재고 2
        given(cartRepository.findByMemberIdForUpdate(MEMBER_ID)).willReturn(Optional.of(cartWithLine(1)));
        given(skuRepository.findById(SKU_ID)).willReturn(Optional.of(sku(2)));
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product(ProductStatus.ON_SALE)));
        given(brandRepository.findById(2L)).willReturn(Optional.of(brand(BrandStatus.ACTIVE)));

        // when & then — 5 > 2
        assertThatThrownBy(() -> useCase.changeQuantity(command(5)))
            .isInstanceOf(CoreException.class)
            .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        then(cartRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("장바구니에 없는 SKU를 변경하면 NOT_FOUND 예외가 발생한다")
    void should_throwNotFound_when_lineAbsent() {
        // given — 카트는 있으나 해당 SKU 라인이 없음
        given(cartRepository.findByMemberIdForUpdate(MEMBER_ID)).willReturn(Optional.of(Cart.create(MEMBER_ID)));
        given(skuRepository.findById(SKU_ID)).willReturn(Optional.of(sku(100)));
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product(ProductStatus.ON_SALE)));
        given(brandRepository.findById(2L)).willReturn(Optional.of(brand(BrandStatus.ACTIVE)));

        // when & then
        assertThatThrownBy(() -> useCase.changeQuantity(command(2)))
            .isInstanceOf(CoreException.class)
            .extracting("errorType").isEqualTo(ErrorType.NOT_FOUND);
        then(cartRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("브랜드가 비활성이면 BAD_REQUEST 예외가 발생하고 저장하지 않는다")
    void should_throwBadRequest_when_brandInactive() {
        // given
        given(cartRepository.findByMemberIdForUpdate(MEMBER_ID)).willReturn(Optional.of(cartWithLine(1)));
        given(skuRepository.findById(SKU_ID)).willReturn(Optional.of(sku(100)));
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product(ProductStatus.ON_SALE)));
        given(brandRepository.findById(2L)).willReturn(Optional.of(brand(BrandStatus.INACTIVE)));

        // when & then
        assertThatThrownBy(() -> useCase.changeQuantity(command(2)))
            .isInstanceOf(CoreException.class)
            .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        then(cartRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("유효하면 수량을 변경하고 저장한 뒤 조립 결과를 반환한다")
    void should_changeAndReturn_when_valid() {
        // given
        given(cartRepository.findByMemberIdForUpdate(MEMBER_ID)).willReturn(Optional.of(cartWithLine(1)));
        given(skuRepository.findById(SKU_ID)).willReturn(Optional.of(sku(100)));
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product(ProductStatus.ON_SALE)));
        given(brandRepository.findById(2L)).willReturn(Optional.of(brand(BrandStatus.ACTIVE)));
        given(cartRepository.save(any(Cart.class))).willAnswer(inv -> inv.getArgument(0));
        CartInfo expected = new CartInfo(MEMBER_ID, List.of(), 0L);
        given(assembler.assemble(any(Cart.class))).willReturn(expected);

        // when
        CartInfo result = useCase.changeQuantity(command(5));

        // then
        assertThat(result).isSameAs(expected);
        then(cartRepository).should().save(any(Cart.class));
    }

}
