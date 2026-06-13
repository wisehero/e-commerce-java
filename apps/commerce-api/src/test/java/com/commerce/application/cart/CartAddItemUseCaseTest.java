package com.commerce.application.cart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.commerce.domain.cart.Cart;
import com.commerce.domain.cart.CartRepository;
import com.commerce.domain.member.Member;
import com.commerce.domain.member.MemberRepository;
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
class CartAddItemUseCaseTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long SKU_ID = 10L;
    private static final Long PRODUCT_ID = 100L;

    @Mock
    private CartRepository cartRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private SkuRepository skuRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private CartInfoAssembler assembler;

    private CartAddItemUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CartAddItemUseCase(cartRepository, memberRepository, skuRepository, productRepository, assembler);
    }

    private CartAddItemCommand command(int quantity) {
        return new CartAddItemCommand(MEMBER_ID, SKU_ID, quantity);
    }

    private Sku sku(int stock) {
        return Sku.reconstitute(SKU_ID, PRODUCT_ID, List.of(new OptionValue("색상", "빨강")),
            new Money(10000), new Money(8000), new Stock(stock));
    }

    private Product product(ProductStatus status) {
        return Product.reconstitute(PRODUCT_ID, "맨투맨", "설명", 1L, 2L, "img.jpg", status);
    }

    private void givenValidCatalog(int stock) {
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(mock(Member.class)));
        given(skuRepository.findById(SKU_ID)).willReturn(Optional.of(sku(stock)));
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product(ProductStatus.ON_SALE)));
    }

    @Test
    @DisplayName("존재하지 않는 회원이면 NOT_FOUND 예외가 발생하고 저장하지 않는다")
    void should_throwNotFound_when_memberMissing() {
        // given
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> useCase.addItem(command(1)))
            .isInstanceOf(CoreException.class)
            .extracting("errorType").isEqualTo(ErrorType.NOT_FOUND);
        then(cartRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 SKU면 NOT_FOUND 예외가 발생한다")
    void should_throwNotFound_when_skuMissing() {
        // given
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(mock(Member.class)));
        given(skuRepository.findById(SKU_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> useCase.addItem(command(1)))
            .isInstanceOf(CoreException.class)
            .extracting("errorType").isEqualTo(ErrorType.NOT_FOUND);
    }

    @Test
    @DisplayName("판매중이 아닌 상품이면 BAD_REQUEST 예외가 발생하고 저장하지 않는다")
    void should_throwBadRequest_when_productNotVisible() {
        // given
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(mock(Member.class)));
        given(skuRepository.findById(SKU_ID)).willReturn(Optional.of(sku(100)));
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product(ProductStatus.SUSPENDED)));

        // when & then
        assertThatThrownBy(() -> useCase.addItem(command(1)))
            .isInstanceOf(CoreException.class)
            .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        then(cartRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("기존 담긴 수량 + 신규 수량이 재고를 초과하면 BAD_REQUEST 예외가 발생한다")
    void should_throwBadRequest_when_cumulativeExceedsStock() {
        // given — 재고 5, 이미 3개 담김
        givenValidCatalog(5);
        Cart cart = Cart.create(MEMBER_ID);
        cart.addItem(SKU_ID, 3);
        given(cartRepository.findByMemberId(MEMBER_ID)).willReturn(Optional.of(cart));

        // when & then — 3 + 3 = 6 > 5
        assertThatThrownBy(() -> useCase.addItem(command(3)))
            .isInstanceOf(CoreException.class)
            .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        then(cartRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("카트가 없으면 새로 만들어 담고 저장한 뒤 조립 결과를 반환한다")
    void should_createCartAndSave_when_noExistingCart() {
        // given
        givenValidCatalog(100);
        given(cartRepository.findByMemberId(MEMBER_ID)).willReturn(Optional.empty());
        given(cartRepository.save(any(Cart.class))).willAnswer(inv -> inv.getArgument(0));
        CartInfo expected = new CartInfo(MEMBER_ID, List.of(), 0L);
        given(assembler.assemble(any(Cart.class))).willReturn(expected);

        // when
        CartInfo result = useCase.addItem(command(2));

        // then
        assertThat(result).isSameAs(expected);
        then(cartRepository).should().save(any(Cart.class));
    }
}
