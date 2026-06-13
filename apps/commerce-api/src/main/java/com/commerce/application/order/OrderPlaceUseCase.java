package com.commerce.application.order;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.commerce.domain.member.MemberRepository;
import com.commerce.domain.order.Order;
import com.commerce.domain.order.OrderLine;
import com.commerce.domain.order.OrderRepository;
import com.commerce.domain.order.PaymentGateway;
import com.commerce.domain.order.PaymentResult;
import com.commerce.domain.product.OptionValue;
import com.commerce.domain.product.Product;
import com.commerce.domain.product.ProductRepository;
import com.commerce.domain.product.Sku;
import com.commerce.domain.product.SkuRepository;
import com.commerce.domain.product.StockDeducter;
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
    private final ProductRepository productRepository;
    private final SkuRepository skuRepository;
    private final OrderRepository orderRepository;
    private final Map<String, StockDeducter> stockDeducters;
    private final PaymentGateway paymentGateway;
    private final TransactionTemplate transactionTemplate;

    public OrderPlaceUseCase(MemberRepository memberRepository, ProductRepository productRepository,
        SkuRepository skuRepository, OrderRepository orderRepository,
        Map<String, StockDeducter> stockDeducters, PaymentGateway paymentGateway,
        PlatformTransactionManager transactionManager) {
        this.memberRepository = memberRepository;
        this.productRepository = productRepository;
        this.skuRepository = skuRepository;
        this.orderRepository = orderRepository;
        this.stockDeducters = stockDeducters;
        this.paymentGateway = paymentGateway;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public OrderInfo place(OrderPlaceCommand command) {
        StockDeducter stockDeducter = resolveStrategy(command.lockMode());

        // Txn1: 검증 + 스냅샷 + 재고 차감 + 주문(PAYMENT_PENDING) 저장 → commit 시 SKU 락 해제
        Order pending = transactionTemplate.execute(status -> openPendingOrder(command, stockDeducter));

        // 결제 호출 — 트랜잭션 밖 (락 미점유)
        PaymentResult result = paymentGateway.pay(pending.getId(), pending.getTotalAmount());

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
        memberRepository.findById(command.memberId())
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 회원입니다."));

        List<OrderLine> lines = new ArrayList<>();
        for (OrderPlaceCommand.LineCommand lineCommand : command.lines()) {
            Sku sku = skuRepository.findById(lineCommand.skuId())
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 SKU입니다."));
            Product product = productRepository.findById(sku.getProductId())
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 상품입니다."));
            if (!product.isVisible()) {
                throw new CoreException(ErrorType.BAD_REQUEST, "구매할 수 없는 상품입니다.");
            }

            lines.add(OrderLine.create(
                product.getId(), sku.getId(), product.getName(),
                summarizeOptions(sku.getOptionValues()), sku.getSalePrice(), lineCommand.quantity()
            ));
            stockDeducter.deduct(sku.getId(), lineCommand.quantity());
        }

        Order order = Order.place(command.memberId(), lines);
        return orderRepository.save(order);
    }

    private Order settlePayment(Long orderId, PaymentResult result) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 주문입니다."));
        if (result.approved()) {
            order.markPaid();
        } else {
            order.cancel();
            restoreStock(order);
        }
        return orderRepository.save(order);
    }

    private void restoreStock(Order order) {
        for (OrderLine line : order.getOrderLines()) {
            Sku sku = skuRepository.findById(line.getSkuId())
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 SKU입니다."));
            sku.restock(line.getQuantity());
            skuRepository.save(sku);
        }
    }

    private String summarizeOptions(List<OptionValue> optionValues) {
        return optionValues.stream()
            .map(option -> option.name() + ":" + option.value())
            .collect(Collectors.joining(" / "));
    }
}
