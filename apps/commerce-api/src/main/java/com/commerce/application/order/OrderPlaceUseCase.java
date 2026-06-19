package com.commerce.application.order;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.commerce.domain.brand.Brand;
import com.commerce.domain.brand.BrandRepository;
import com.commerce.domain.category.CategoryRepository;
import com.commerce.domain.coupon.ApplicabilityScope;
import com.commerce.domain.coupon.DiscountableLine;
import com.commerce.domain.coupon.IssuedCoupon;
import com.commerce.domain.coupon.IssuedCouponRepository;
import com.commerce.domain.coupon.ScopeType;
import com.commerce.domain.member.MemberRepository;
import com.commerce.domain.order.Order;
import com.commerce.domain.order.OrderLine;
import com.commerce.domain.order.OrderRepository;
import com.commerce.domain.order.PaymentGateway;
import com.commerce.domain.order.PaymentResult;
import com.commerce.domain.product.Product;
import com.commerce.domain.product.ProductRepository;
import com.commerce.domain.product.Sku;
import com.commerce.domain.product.SkuRepository;
import com.commerce.domain.product.StockDeducter;
import com.commerce.domain.shared.Money;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

/**
 * 주문 생성 유스케이스.
 *
 * <p>2단계 동기 흐름이다: Txn1(검증·스냅샷·재고차감·주문생성) → 결제 호출(트랜잭션 밖) → Txn2(결제 결과 반영).
 * 외부 결제 호출이 재고 차감 락을 점유하지 않도록 트랜잭션 경계를 명시하려고 TransactionTemplate을 쓴다
 * (같은 빈 내 @Transactional 자기호출은 프록시를 거치지 않아 동작하지 않으므로).
 */
@Service
public class OrderPlaceUseCase {

    private final MemberRepository memberRepository;
    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;
    private final SkuRepository skuRepository;
    private final CategoryRepository categoryRepository;
    private final OrderRepository orderRepository;
    private final IssuedCouponRepository issuedCouponRepository;
    private final OrderCompensationHelper orderCompensationHelper;
    private final Map<String, StockDeducter> stockDeducters;
    private final PaymentGateway paymentGateway;
    private final TransactionTemplate transactionTemplate;

    public OrderPlaceUseCase(MemberRepository memberRepository, BrandRepository brandRepository,
        ProductRepository productRepository,
        SkuRepository skuRepository, CategoryRepository categoryRepository, OrderRepository orderRepository,
        IssuedCouponRepository issuedCouponRepository,
        OrderCompensationHelper orderCompensationHelper,
        Map<String, StockDeducter> stockDeducters, PaymentGateway paymentGateway,
        PlatformTransactionManager transactionManager) {
        this.memberRepository = memberRepository;
        this.brandRepository = brandRepository;
        this.productRepository = productRepository;
        this.skuRepository = skuRepository;
        this.categoryRepository = categoryRepository;
        this.orderRepository = orderRepository;
        this.issuedCouponRepository = issuedCouponRepository;
        this.orderCompensationHelper = orderCompensationHelper;
        this.stockDeducters = stockDeducters;
        this.paymentGateway = paymentGateway;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public OrderInfo place(OrderPlaceCommand command) {
        StockDeducter stockDeducter = resolveStrategy(command.lockMode());

        // Txn1: 검증 + 스냅샷 + 재고 차감 + 주문(PAYMENT_PENDING) 저장 → commit 시 SKU 락 해제
        Order pending = transactionTemplate.execute(status -> openPendingOrder(command, stockDeducter));

        // 결제 호출 — 트랜잭션 밖 (락 미점유)
        PaymentResult result = pending.getPayableAmount().isZero()
            ? PaymentResult.success()
            : paymentGateway.pay(pending.getId(), pending.getPayableAmount());

        // Txn2: 결제 결과 반영 (성공 → PAID / 실패 → CANCELLED + 재고 복원)
        Order settled = transactionTemplate.execute(status -> settlePayment(pending.getId(), result));

        return OrderInfo.from(settled);
    }

    private StockDeducter resolveStrategy(String lockMode) {
        StockDeducter deducter = stockDeducters.get(lockMode);
        if (deducter == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "지원하지 않는 재고 차감 방식입니다: " + lockMode);
        }
        return deducter;
    }

    private Order openPendingOrder(OrderPlaceCommand command, StockDeducter stockDeducter) {
        ensureMemberExists(command.memberId());

        List<PreparedLine> prepared = createOrderLinesAndDeductStock(command, stockDeducter);
        List<OrderLine> lines = prepared.stream().map(PreparedLine::orderLine).toList();
        List<DiscountableLine> discountableLines = prepared.stream().map(PreparedLine::discountableLine).toList();

        IssuedCoupon coupon = findCoupon(command.couponId());
        Set<Long> resolvedCategoryIds = resolveCategoryIds(coupon);
        Money discount = coupon == null
            ? Money.ZERO
            : coupon.calculateDiscount(discountableLines, resolvedCategoryIds);

        Order order = Order.place(command.memberId(), lines, discount, command.couponId());
        Order saved = orderRepository.save(order);
        markCouponUsedIfPresent(coupon, command.memberId(), discountableLines, resolvedCategoryIds, saved.getId());
        return saved;
    }

    private void ensureMemberExists(Long memberId) {
        memberRepository.findById(memberId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 회원입니다."));
    }

    private List<PreparedLine> createOrderLinesAndDeductStock(OrderPlaceCommand command, StockDeducter stockDeducter) {
        List<PreparedLine> prepared = new ArrayList<>();
        for (OrderPlaceCommand.LineCommand lineCommand : command.lines()) {
            prepared.add(createOrderLineAndDeductStock(lineCommand, stockDeducter));
        }
        return prepared;
    }

    private PreparedLine createOrderLineAndDeductStock(OrderPlaceCommand.LineCommand lineCommand,
        StockDeducter stockDeducter) {
        Sku sku = skuRepository.findById(lineCommand.skuId())
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 SKU입니다."));
        Product product = productRepository.findById(sku.getProductId())
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 상품입니다."));
        if (!product.isVisible()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "구매할 수 없는 상품입니다.");
        }
        Brand brand = brandRepository.findById(product.getBrandId())
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 브랜드입니다."));
        if (!brand.isVisible()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "구매할 수 없는 브랜드입니다.");
        }

        OrderLine orderLine = OrderLine.create(
            product.getId(), sku.getId(), product.getName(),
            sku.optionSummary(), sku.getSalePrice(), lineCommand.quantity()
        );
        stockDeducter.deduct(sku.getId(), lineCommand.quantity());

        // 쿠폰 scope 매칭용 중립 입력. 박제하지 않고 이 트랜잭션에서만 쓰고 버린다.
        DiscountableLine discountableLine = new DiscountableLine(
            orderLine.lineAmount(), product.getId(), product.getBrandId(), product.getCategoryId());
        return new PreparedLine(orderLine, discountableLine);
    }

    /** CATEGORY scope면 대상 카테고리의 서브트리 id 집합을 주문 시점에 신선 해소한다. 그 외엔 빈 집합. */
    private Set<Long> resolveCategoryIds(IssuedCoupon coupon) {
        if (coupon == null) {
            return Set.of();
        }
        ApplicabilityScope scope = coupon.getApplicabilityScope();
        if (scope.type() == ScopeType.CATEGORY) {
            return Set.copyOf(categoryRepository.findSelfAndDescendantIds(scope.targetId()));
        }
        return Set.of();
    }

    private void markCouponUsedIfPresent(IssuedCoupon coupon, Long memberId, List<DiscountableLine> lines,
        Set<Long> resolvedCategoryIds, Long orderId) {
        if (coupon != null) {
            ZonedDateTime now = ZonedDateTime.now();
            coupon.use(memberId, lines, resolvedCategoryIds, now, orderId);
            if (!issuedCouponRepository.markUsedIfAvailable(
                coupon.getId(), memberId, now, orderId)) {
                throw new CoreException(ErrorType.BAD_REQUEST, "이미 사용했거나 만료된 쿠폰입니다.");
            }
        }
    }

    private Order settlePayment(Long orderId, PaymentResult result) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 주문입니다."));
        if (result.approved()) {
            order.markPaid();
        } else {
            order.cancel();
            orderCompensationHelper.restore(order);
        }
        return orderRepository.save(order);
    }

    private IssuedCoupon findCoupon(Long couponId) {
        if (couponId == null) {
            return null;
        }
        return issuedCouponRepository.findById(couponId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 쿠폰입니다."));
    }

    /** 주문 라인 생성 결과: 영속용 OrderLine과 쿠폰 계산용 DiscountableLine 한 쌍. */
    private record PreparedLine(OrderLine orderLine, DiscountableLine discountableLine) {
    }

}
