package com.commerce.infrastructure.cart;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.commerce.domain.cart.Cart;
import com.commerce.domain.cart.CartRepository;
import com.commerce.support.IntegrationTestSupport;

/**
 * 장바구니 영속성 통합 테스트. Cart Aggregate가 두 테이블(carts·cart_lines)에 걸쳐
 * 저장·조립되는지, 업데이트 시 라인이 전량 교체되는지를 실 MySQL로 검증한다
 * (@OneToMany 없이 CartRepositoryImpl이 수동 조립).
 */
class CartPersistenceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartJpaRepository cartJpaRepository;

    @Autowired
    private CartLineJpaRepository cartLineJpaRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate txTemplate;

    @BeforeEach
    void setUp() {
        txTemplate = new TransactionTemplate(transactionManager);
    }

    @AfterEach
    void tearDown() {
        cartLineJpaRepository.deleteAll();
        cartJpaRepository.deleteAll();
    }

    private Cart sampleCart(Long memberId) {
        Cart cart = Cart.create(memberId);
        cart.addItem(10L, 2);
        cart.addItem(11L, 1);
        return cart;
    }

    @Test
    @DisplayName("장바구니를 두 테이블에 저장하고 라인까지 다시 조립한다")
    void should_persistAndReassemble_when_save() {
        // when
        Cart saved = cartRepository.save(sampleCart(1L));

        // then
        assertThat(saved.getId()).isNotNull();

        Cart found = cartRepository.findByMemberId(1L).orElseThrow();
        assertThat(found.getMemberId()).isEqualTo(1L);
        assertThat(found.getLines()).hasSize(2);
        assertThat(found.getLines()).allSatisfy(line -> assertThat(line.getId()).isNotNull());
        assertThat(found.quantityOf(10L)).isEqualTo(2);
        assertThat(found.quantityOf(11L)).isEqualTo(1);
    }

    @Test
    @DisplayName("업데이트 시 라인을 전량 교체한다 (병합·제거·추가가 중복 없이 반영)")
    void should_replaceLines_when_update() {
        // given
        cartRepository.save(sampleCart(1L));

        // when — 재조회 후 변경(병합·제거·신규)하고 저장
        txTemplate.executeWithoutResult(s -> {
            Cart cart = cartRepository.findByMemberId(1L).orElseThrow();
            cart.addItem(10L, 3);      // 2 + 3 = 5 (병합)
            cart.removeItem(11L);      // 제거
            cart.addItem(12L, 1);      // 신규
            cartRepository.save(cart);
        });

        // then
        Cart reloaded = cartRepository.findByMemberId(1L).orElseThrow();
        assertThat(reloaded.getLines()).hasSize(2);
        assertThat(reloaded.quantityOf(10L)).isEqualTo(5);
        assertThat(reloaded.quantityOf(11L)).isZero();
        assertThat(reloaded.quantityOf(12L)).isEqualTo(1);
        // 라인 중복 없이 정확히 2행
        assertThat(cartLineJpaRepository.findByCartId(reloaded.getId())).hasSize(2);
    }

    @Test
    @DisplayName("비운 뒤 저장하면 라인이 모두 삭제된다")
    void should_removeAllLines_when_clearedAndSaved() {
        // given
        cartRepository.save(sampleCart(1L));

        // when
        txTemplate.executeWithoutResult(s -> {
            Cart cart = cartRepository.findByMemberId(1L).orElseThrow();
            cart.clear();
            cartRepository.save(cart);
        });

        // then
        Cart reloaded = cartRepository.findByMemberId(1L).orElseThrow();
        assertThat(reloaded.isEmpty()).isTrue();
        assertThat(cartLineJpaRepository.findByCartId(reloaded.getId())).isEmpty();
    }
}
