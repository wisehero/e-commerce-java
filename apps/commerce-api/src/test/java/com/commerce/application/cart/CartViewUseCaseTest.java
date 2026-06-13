package com.commerce.application.cart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.commerce.domain.cart.Cart;
import com.commerce.domain.cart.CartLine;
import com.commerce.domain.cart.CartRepository;

@ExtendWith(MockitoExtension.class)
class CartViewUseCaseTest {

    private static final Long MEMBER_ID = 1L;

    @Mock
    private CartRepository cartRepository;
    @Mock
    private CartInfoAssembler assembler;

    private CartViewUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CartViewUseCase(cartRepository, assembler);
    }

    @Test
    @DisplayName("카트가 없으면 404가 아니라 빈 카트를 조립해 반환한다")
    void should_returnEmptyView_when_noCart() {
        // given
        given(cartRepository.findByMemberId(MEMBER_ID)).willReturn(Optional.empty());
        CartInfo empty = new CartInfo(MEMBER_ID, List.of(), 0L);
        given(assembler.assemble(any(Cart.class))).willReturn(empty);

        // when
        CartInfo result = useCase.view(MEMBER_ID);

        // then
        assertThat(result).isSameAs(empty);
        then(assembler).should().assemble(any(Cart.class));
    }

    @Test
    @DisplayName("카트가 있으면 해당 카트를 조립해 반환한다")
    void should_assembleExistingCart_when_present() {
        // given
        Cart cart = Cart.reconstitute(5L, MEMBER_ID, List.of(CartLine.reconstitute(1L, 10L, 2)));
        given(cartRepository.findByMemberId(MEMBER_ID)).willReturn(Optional.of(cart));
        CartInfo expected = new CartInfo(MEMBER_ID, List.of(), 16000L);
        given(assembler.assemble(cart)).willReturn(expected);

        // when
        CartInfo result = useCase.view(MEMBER_ID);

        // then
        assertThat(result).isSameAs(expected);
    }
}
