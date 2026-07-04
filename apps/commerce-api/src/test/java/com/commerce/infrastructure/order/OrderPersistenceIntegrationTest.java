package com.commerce.infrastructure.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.commerce.domain.order.Order;
import com.commerce.domain.order.OrderLine;
import com.commerce.domain.order.OrderRepository;
import com.commerce.domain.order.OrderStatus;
import com.commerce.domain.shared.Money;
import com.commerce.support.IntegrationTestSupport;
import com.commerce.support.page.PageResult;

/**
 * 주문 영속성 통합 테스트. Order Aggregate가 두 테이블(orders·order_lines)에 걸쳐
 * 저장·조립되는지를 실 MySQL로 검증한다(@OneToMany 없이 OrderRepositoryImpl이 수동 조립).
 */
class OrderPersistenceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderJpaRepository orderJpaRepository;

    @Autowired
    private OrderLineJpaRepository orderLineJpaRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate txTemplate;

    @BeforeEach
    void setUp() {
        txTemplate = new TransactionTemplate(transactionManager);
    }

    @AfterEach
    void tearDown() {
        orderLineJpaRepository.deleteAll();
        orderJpaRepository.deleteAll();
    }

    private Order sampleOrder(Long memberId) {
        OrderLine line1 = OrderLine.create(100L, 10L, "맨투맨", "색상:블랙", new Money(8000), 2);   // 16000
        OrderLine line2 = OrderLine.create(101L, 11L, "청바지", "사이즈:32", new Money(30000), 1);  // 30000
        return Order.place(memberId, List.of(line1, line2));
    }

    private Order cartSourceOrder(Long memberId, Long sourceCartId) {
        OrderLine line = OrderLine.create(100L, 10L, "맨투맨", "색상:블랙", new Money(8000), 2);
        return Order.place(memberId, List.of(line), Money.ZERO, null, sourceCartId);
    }

    @Test
    @DisplayName("주문을 두 테이블에 저장하고 라인까지 다시 조립한다")
    void should_persistAndReassemble_when_save() {
        // when
        Order saved = orderRepository.save(sampleOrder(1L));

        // then
        assertThat(saved.getId()).isNotNull();

        Order found = orderRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getMemberId()).isEqualTo(1L);
        assertThat(found.getStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);
        assertThat(found.getTotalAmount()).isEqualTo(new Money(46000));   // 라인 합
        assertThat(found.getOrderLines()).hasSize(2);
        assertThat(found.getOrderLines()).allSatisfy(line -> assertThat(line.getId()).isNotNull());
    }

    @Test
    @DisplayName("카트 기반 주문의 sourceCartId를 저장하고 복원한다")
    void should_persistSourceCartId_when_cartCheckoutOrder() {
        // when
        Order saved = orderRepository.save(cartSourceOrder(1L, 100L));

        // then
        Order found = orderRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getSourceCartId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("결제 완료 상태 전이가 영속화된다 (라인은 불변)")
    void should_persistStatusChange_when_markPaid() {
        // given
        Long orderId = orderRepository.save(sampleOrder(1L)).getId();

        // when — 상태 변경 save는 더티체킹에 기대므로 하나의 트랜잭션 안에서 수행
        txTemplate.executeWithoutResult(s -> {
            Order order = orderRepository.findById(orderId).orElseThrow();
            order.markPaid();
            orderRepository.save(order);
        });

        // then
        Order reloaded = orderRepository.findById(orderId).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(reloaded.getOrderLines()).hasSize(2);
    }

    @Test
    @DisplayName("회원별 목록을 페이지로 조회한다 (다른 회원 주문은 제외)")
    void should_returnMemberOrders_when_findByMemberId() {
        // given
        orderRepository.save(sampleOrder(1L));
        orderRepository.save(sampleOrder(1L));
        orderRepository.save(sampleOrder(2L));

        // when
        PageResult<Order> page = orderRepository.findByMemberId(1L, 0, 10);

        // then
        assertThat(page.totalCount()).isEqualTo(2);
        assertThat(page.items()).hasSize(2);
        assertThat(page.items()).allSatisfy(order -> assertThat(order.getMemberId()).isEqualTo(1L));
    }
}
