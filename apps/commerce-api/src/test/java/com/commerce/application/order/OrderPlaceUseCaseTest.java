package com.commerce.application.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import com.commerce.application.purchase.PurchasableItemResolver;
import com.commerce.domain.brand.Brand;
import com.commerce.domain.brand.BrandRepository;
import com.commerce.domain.brand.BrandStatus;
import com.commerce.domain.category.CategoryRepository;
import com.commerce.domain.coupon.ApplicabilityScope;
import com.commerce.domain.coupon.DiscountRule;
import com.commerce.domain.coupon.DiscountType;
import com.commerce.domain.coupon.IssuedCoupon;
import com.commerce.domain.coupon.IssuedCouponRepository;
import com.commerce.domain.member.Member;
import com.commerce.domain.member.MemberRepository;
import com.commerce.domain.order.Order;
import com.commerce.domain.order.OrderLine;
import com.commerce.domain.order.OrderRepository;
import com.commerce.domain.order.OrderStatus;
import com.commerce.domain.order.PaymentGateway;
import com.commerce.domain.order.PaymentResult;
import com.commerce.domain.product.OptionValue;
import com.commerce.domain.product.Product;
import com.commerce.domain.product.ProductRepository;
import com.commerce.domain.product.ProductStatus;
import com.commerce.domain.product.Sku;
import com.commerce.domain.product.SkuRepository;
import com.commerce.domain.product.StockDeducter;
import com.commerce.domain.shared.Money;
import com.commerce.domain.product.Stock;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

@ExtendWith(MockitoExtension.class)
class OrderPlaceUseCaseTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long SKU_ID = 10L;
    private static final Long PRODUCT_ID = 100L;
    private static final Long ORDER_ID = 1000L;
    private static final Long COUPON_ID = 50L;

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private BrandRepository brandRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private SkuRepository skuRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private IssuedCouponRepository issuedCouponRepository;
    @Mock
    private StockDeducter stockDeducter;
    @Mock
    private PaymentGateway paymentGateway;
    @Mock
    private PlatformTransactionManager transactionManager;

    private OrderPlaceUseCase useCase;
    private OrderCompensationHelper orderCompensationHelper;

    @BeforeEach
    void setUp() {
        lenient().when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        orderCompensationHelper = new OrderCompensationHelper(skuRepository, issuedCouponRepository);
        PurchasableItemResolver purchasableItemResolver = new PurchasableItemResolver(skuRepository, productRepository,
            brandRepository);
        OrderLinePreparer orderLinePreparer = new OrderLinePreparer(purchasableItemResolver,
            Map.of("optimistic", stockDeducter));
        OrderCouponApplier orderCouponApplier = new OrderCouponApplier(issuedCouponRepository, categoryRepository);
        useCase = new OrderPlaceUseCase(memberRepository, orderRepository, orderLinePreparer, orderCouponApplier,
            orderCompensationHelper, paymentGateway, new TransactionTemplate(transactionManager));
    }

    private OrderPlaceCommand command(String lockMode) {
        return new OrderPlaceCommand(MEMBER_ID, List.of(new OrderPlaceCommand.LineCommand(SKU_ID, 2)), lockMode);
    }

    private Sku sku() {
        return Sku.reconstitute(SKU_ID, PRODUCT_ID, List.of(new OptionValue("색상", "빨강")),
            new Money(10000), new Money(8000), new Stock(100));
    }

    private Product product(ProductStatus status) {
        return Product.reconstitute(PRODUCT_ID, "맨투맨", "설명", 1L, 2L, "img.jpg", status);
    }

    private Brand brand(BrandStatus status) {
        return Brand.reconstitute(2L, "나이키", "logo.jpg", status);
    }

    private Order pendingOrder() {
        OrderLine line = OrderLine.reconstitute(1L, PRODUCT_ID, SKU_ID, "맨투맨", "색상:빨강", new Money(8000), 2);
        return Order.reconstitute(ORDER_ID, MEMBER_ID, OrderStatus.PAYMENT_PENDING, List.of(line), new Money(16000));
    }

    private IssuedCoupon coupon() {
        return IssuedCoupon.reconstitute(COUPON_ID, 20L, MEMBER_ID,
            ApplicabilityScope.whole(),
            new DiscountRule(DiscountType.FIXED, 5000L, null, Money.ZERO),
            com.commerce.domain.coupon.CouponStatus.UNUSED,
            java.time.ZonedDateTime.now().minusDays(1),
            java.time.ZonedDateTime.now().plusDays(1),
            null);
    }

    private void givenValidCatalog() {
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(mock(Member.class)));
        given(skuRepository.findById(SKU_ID)).willReturn(Optional.of(sku()));
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product(ProductStatus.ON_SALE)));
        given(brandRepository.findById(2L)).willReturn(Optional.of(brand(BrandStatus.ACTIVE)));
        given(orderRepository.save(any(Order.class))).willAnswer(inv -> {
            Order o = inv.getArgument(0);
            return Order.reconstitute(ORDER_ID, o.getMemberId(), o.getStatus(), o.getOrderLines(),
                o.getTotalAmount(), o.getDiscountAmount(), o.getPayableAmount(), o.getUsedCouponId());
        });
        given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(pendingOrder()));
    }

    private OrderPlaceCommand couponCommand(String lockMode) {
        return new OrderPlaceCommand(MEMBER_ID, List.of(new OrderPlaceCommand.LineCommand(SKU_ID, 2)),
            lockMode, COUPON_ID);
    }

    @Nested
    @DisplayName("주문 생성 — 결제 성공")
    class PaymentSuccess {

        @Test
        @DisplayName("재고를 차감하고 결제 성공 시 PAID 주문을 반환한다")
        void should_returnPaidOrder_when_paymentSucceeds() {
            // given
            givenValidCatalog();
            given(paymentGateway.pay(eq(ORDER_ID), any())).willReturn(PaymentResult.success());

            // when
            OrderInfo info = useCase.place(command("optimistic"));

            // then
            assertThat(info.status()).isEqualTo("PAID");
            assertThat(info.totalAmount()).isEqualTo(16000L);
            then(stockDeducter).should().deduct(SKU_ID, 2);
            then(paymentGateway).should().pay(eq(ORDER_ID), any());
            then(paymentGateway).should(never()).refund(any(), any());
        }

        @Test
        @DisplayName("쿠폰을 적용하면 청구액으로 결제하고 쿠폰을 사용 처리한다")
        void should_payPayableAmountAndUseCoupon_when_couponApplied() {
            // given
            givenValidCatalog();
            given(issuedCouponRepository.findById(COUPON_ID)).willReturn(Optional.of(coupon()));
            given(issuedCouponRepository.markUsedIfAvailable(eq(COUPON_ID), eq(MEMBER_ID), any(), eq(ORDER_ID)))
                .willReturn(true);
            given(paymentGateway.pay(eq(ORDER_ID), eq(new Money(11000)))).willReturn(PaymentResult.success());
            given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(
                Order.reconstitute(ORDER_ID, MEMBER_ID, OrderStatus.PAYMENT_PENDING,
                    pendingOrder().getOrderLines(), new Money(16000), new Money(5000), new Money(11000), COUPON_ID)));

            // when
            OrderInfo info = useCase.place(couponCommand("optimistic"));

            // then
            assertThat(info.discountAmount()).isEqualTo(5000L);
            assertThat(info.payableAmount()).isEqualTo(11000L);
            assertThat(info.usedCouponId()).isEqualTo(COUPON_ID);
            then(paymentGateway).should().pay(ORDER_ID, new Money(11000));
            then(issuedCouponRepository).should().markUsedIfAvailable(eq(COUPON_ID), eq(MEMBER_ID), any(), eq(ORDER_ID));
        }
    }

    @Nested
    @DisplayName("주문 생성 — 결제 실패 보상")
    class PaymentFailure {

        @Test
        @DisplayName("결제 실패 시 주문을 취소하고 재고를 복원한다")
        void should_cancelAndRestoreStock_when_paymentFails() {
            // given
            givenValidCatalog();
            given(paymentGateway.pay(eq(ORDER_ID), any())).willReturn(PaymentResult.failure());
            given(skuRepository.save(any(Sku.class))).willAnswer(inv -> inv.getArgument(0));

            // when
            OrderInfo info = useCase.place(command("optimistic"));

            // then
            assertThat(info.status()).isEqualTo("CANCELLED");
            then(skuRepository).should().save(any(Sku.class));         // 재고 복원
            then(paymentGateway).should(never()).refund(any(), any()); // 결제 실패라 환불 없음
        }

        @Test
        @DisplayName("쿠폰 적용 주문의 결제 실패 시 쿠폰을 복원한다")
        void should_restoreCoupon_when_couponOrderPaymentFails() {
            // given
            givenValidCatalog();
            given(issuedCouponRepository.findById(COUPON_ID)).willReturn(Optional.of(coupon()));
            given(issuedCouponRepository.markUsedIfAvailable(eq(COUPON_ID), eq(MEMBER_ID), any(), eq(ORDER_ID)))
                .willReturn(true);
            given(paymentGateway.pay(eq(ORDER_ID), any())).willReturn(PaymentResult.failure());
            given(skuRepository.save(any(Sku.class))).willAnswer(inv -> inv.getArgument(0));
            given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(
                Order.reconstitute(ORDER_ID, MEMBER_ID, OrderStatus.PAYMENT_PENDING,
                    pendingOrder().getOrderLines(), new Money(16000), new Money(5000), new Money(11000), COUPON_ID)));

            // when
            OrderInfo info = useCase.place(couponCommand("optimistic"));

            // then
            assertThat(info.status()).isEqualTo("CANCELLED");
            then(issuedCouponRepository).should().restoreByOrderId(ORDER_ID);
        }
    }

    @Nested
    @DisplayName("주문 생성 — 검증 실패")
    class Validation {

        @Test
        @DisplayName("지원하지 않는 lockMode면 BAD_REQUEST 예외가 발생한다")
        void should_throwBadRequest_when_unknownLockMode() {
            // when & then
            assertThatThrownBy(() -> useCase.place(command("unknown")))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
            then(orderRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("존재하지 않는 회원이면 NOT_FOUND 예외가 발생한다")
        void should_throwNotFound_when_memberMissing() {
            // given
            given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> useCase.place(command("optimistic")))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.NOT_FOUND);
        }

        @Test
        @DisplayName("ON_SALE가 아닌 상품이면 BAD_REQUEST 예외가 발생한다")
        void should_throwBadRequest_when_productNotVisible() {
            // given
            given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(mock(Member.class)));
            given(skuRepository.findById(SKU_ID)).willReturn(Optional.of(sku()));
            given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product(ProductStatus.SUSPENDED)));

            // when & then
            assertThatThrownBy(() -> useCase.place(command("optimistic")))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
            then(orderRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("브랜드가 비활성이면 BAD_REQUEST 예외가 발생한다")
        void should_throwBadRequest_when_brandInactive() {
            // given
            given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(mock(Member.class)));
            given(skuRepository.findById(SKU_ID)).willReturn(Optional.of(sku()));
            given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product(ProductStatus.ON_SALE)));
            given(brandRepository.findById(2L)).willReturn(Optional.of(brand(BrandStatus.INACTIVE)));

            // when & then
            assertThatThrownBy(() -> useCase.place(command("optimistic")))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
            then(orderRepository).should(never()).save(any());
        }
    }
}
