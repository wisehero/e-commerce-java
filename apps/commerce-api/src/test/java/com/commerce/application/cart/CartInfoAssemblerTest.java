package com.commerce.application.cart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.commerce.domain.brand.Brand;
import com.commerce.domain.brand.BrandRepository;
import com.commerce.domain.brand.BrandStatus;
import com.commerce.domain.cart.Cart;
import com.commerce.domain.product.OptionValue;
import com.commerce.domain.product.Product;
import com.commerce.domain.product.ProductRepository;
import com.commerce.domain.product.ProductStatus;
import com.commerce.domain.product.Sku;
import com.commerce.domain.product.SkuRepository;
import com.commerce.domain.product.Stock;
import com.commerce.domain.shared.Money;

@ExtendWith(MockitoExtension.class)
class CartInfoAssemblerTest {

    private static final Long MEMBER_ID = 1L;

    @Mock
    private SkuRepository skuRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private BrandRepository brandRepository;

    private CartInfoAssembler assembler;

    @BeforeEach
    void setUp() {
        assembler = new CartInfoAssembler(skuRepository, productRepository, brandRepository);
    }

    private Sku sku(long skuId, long productId, long salePrice, int stock) {
        return Sku.reconstitute(skuId, productId, List.of(new OptionValue("색상", "빨강")),
            new Money(salePrice), new Money(salePrice), new Stock(stock));
    }

    private Product product(long productId, ProductStatus status) {
        return Product.reconstitute(productId, "맨투맨" + productId, "설명", 1L, 2L, "img.jpg", status);
    }

    private Brand brand(BrandStatus status) {
        return Brand.reconstitute(2L, "나이키", "logo.jpg", status);
    }

    private Cart cartWith(long... skuIds) {
        Cart cart = Cart.create(MEMBER_ID);
        for (long skuId : skuIds) {
            cart.addItem(skuId, 2);
        }
        return cart;
    }

    @Test
    @DisplayName("빈 카트는 총액 0이고 어떤 조회도 하지 않는다")
    void should_returnEmpty_when_cartEmpty() {
        // when
        CartInfo info = assembler.assemble(Cart.create(MEMBER_ID));

        // then
        assertThat(info)
            .satisfies(i -> assertThat(i.lines()).isEmpty())
            .satisfies(i -> assertThat(i.cartTotal()).isZero());
        then(skuRepository).should(never()).findByIds(anyList());
        then(productRepository).should(never()).findByIds(anyList());
        then(brandRepository).should(never()).findByIds(anyList());
    }

    @Test
    @DisplayName("판매중 + 재고 충분이면 PURCHASABLE이고 총액에 합산된다")
    void should_markPurchasable_when_onSaleAndEnoughStock() {
        // given — salePrice 8000 × 2 = 16000, 재고 5
        given(skuRepository.findByIds(anyList())).willReturn(List.of(sku(10L, 100L, 8000, 5)));
        given(productRepository.findByIds(anyList())).willReturn(List.of(product(100L, ProductStatus.ON_SALE)));
        given(brandRepository.findByIds(anyList())).willReturn(List.of(brand(BrandStatus.ACTIVE)));

        // when
        CartInfo info = assembler.assemble(cartWith(10L));

        // then
        assertThat(info.lines())
            .singleElement()
            .satisfies(line -> assertThat(line.status()).isEqualTo(CartLineStatus.PURCHASABLE))
            .satisfies(line -> assertThat(line.salePrice()).isEqualTo(8000L))
            .satisfies(line -> assertThat(line.lineSubtotal()).isEqualTo(16000L))
            .satisfies(line -> assertThat(line.optionSummary()).isEqualTo("색상:빨강"));
        assertThat(info.cartTotal()).isEqualTo(16000L);
    }

    @Test
    @DisplayName("재고가 담은 수량보다 적으면 OUT_OF_STOCK이고 총액에서 제외된다")
    void should_markOutOfStock_when_stockLessThanQuantity() {
        // given — 담은 수량 2, 재고 1
        given(skuRepository.findByIds(anyList())).willReturn(List.of(sku(10L, 100L, 8000, 1)));
        given(productRepository.findByIds(anyList())).willReturn(List.of(product(100L, ProductStatus.ON_SALE)));
        given(brandRepository.findByIds(anyList())).willReturn(List.of(brand(BrandStatus.ACTIVE)));

        // when
        CartInfo info = assembler.assemble(cartWith(10L));

        // then
        assertThat(info.lines())
            .singleElement()
            .satisfies(line -> assertThat(line.status()).isEqualTo(CartLineStatus.OUT_OF_STOCK));
        assertThat(info.cartTotal()).isZero();
    }

    @Test
    @DisplayName("상품이 판매중지면 UNAVAILABLE이고 총액에서 제외된다")
    void should_markUnavailable_when_productSuspended() {
        // given
        given(skuRepository.findByIds(anyList())).willReturn(List.of(sku(10L, 100L, 8000, 5)));
        given(productRepository.findByIds(anyList())).willReturn(List.of(product(100L, ProductStatus.SUSPENDED)));
        given(brandRepository.findByIds(anyList())).willReturn(List.of(brand(BrandStatus.ACTIVE)));

        // when
        CartInfo info = assembler.assemble(cartWith(10L));

        // then
        assertThat(info.lines())
            .singleElement()
            .satisfies(line -> assertThat(line.status()).isEqualTo(CartLineStatus.UNAVAILABLE));
        assertThat(info.cartTotal()).isZero();
    }

    @Test
    @DisplayName("브랜드가 비활성이면 UNAVAILABLE이고 총액에서 제외된다")
    void should_markUnavailable_when_brandInactive() {
        // given
        given(skuRepository.findByIds(anyList())).willReturn(List.of(sku(10L, 100L, 8000, 5)));
        given(productRepository.findByIds(anyList())).willReturn(List.of(product(100L, ProductStatus.ON_SALE)));
        given(brandRepository.findByIds(anyList())).willReturn(List.of(brand(BrandStatus.INACTIVE)));

        // when
        CartInfo info = assembler.assemble(cartWith(10L));

        // then
        assertThat(info.lines())
            .singleElement()
            .satisfies(line -> assertThat(line.status()).isEqualTo(CartLineStatus.UNAVAILABLE));
        assertThat(info.cartTotal()).isZero();
    }

    @Test
    @DisplayName("상태가 섞이면 PURCHASABLE 라인 소계만 총액에 합산한다")
    void should_sumOnlyPurchasable_when_mixedStatuses() {
        // given — sku10 구매가능(8000×2=16000), sku11 품절, sku12 판매중지
        given(skuRepository.findByIds(anyList())).willReturn(List.of(
            sku(10L, 100L, 8000, 5),     // PURCHASABLE
            sku(11L, 101L, 5000, 1),     // OUT_OF_STOCK (재고 1 < 담은 2)
            sku(12L, 102L, 3000, 5)      // UNAVAILABLE (판매중지)
        ));
        given(productRepository.findByIds(anyList())).willReturn(List.of(
            product(100L, ProductStatus.ON_SALE),
            product(101L, ProductStatus.ON_SALE),
            product(102L, ProductStatus.SUSPENDED)
        ));
        given(brandRepository.findByIds(anyList())).willReturn(List.of(brand(BrandStatus.ACTIVE)));

        // when
        CartInfo info = assembler.assemble(cartWith(10L, 11L, 12L));

        // then
        assertThat(info.lines()).hasSize(3);
        assertThat(info.cartTotal()).isEqualTo(16000L);
    }

    @Test
    @DisplayName("라인이 여러 개여도 Sku·Product를 각각 한 번만 배치 조회한다 (N+1 회피)")
    void should_batchLookupOnce_when_multipleLines() {
        // given
        given(skuRepository.findByIds(anyList())).willReturn(List.of(
            sku(10L, 100L, 8000, 5),
            sku(11L, 100L, 5000, 5)
        ));
        given(productRepository.findByIds(anyList())).willReturn(List.of(product(100L, ProductStatus.ON_SALE)));
        given(brandRepository.findByIds(anyList())).willReturn(List.of(brand(BrandStatus.ACTIVE)));

        // when
        assembler.assemble(cartWith(10L, 11L));

        // then
        then(skuRepository).should().findByIds(anyList());
        then(productRepository).should().findByIds(anyList());
        then(brandRepository).should().findByIds(anyList());
    }
}
