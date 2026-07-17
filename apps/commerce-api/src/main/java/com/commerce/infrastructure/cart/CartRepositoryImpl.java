package com.commerce.infrastructure.cart;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.commerce.domain.cart.Cart;
import com.commerce.domain.cart.CartLine;
import com.commerce.domain.cart.CartRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class CartRepositoryImpl implements CartRepository {

    private final CartJpaRepository cartJpa;
    private final CartLineJpaRepository cartLineJpa;

    @Override
    public Cart save(Cart cart) {
        if (cart.getId() == null) {
            return insert(cart);
        }
        return update(cart);
    }

    /** 신규: carts 저장 → 생성된 id로 cart_lines 저장 → 조립 */
    private Cart insert(Cart cart) {
        CartJpaEntity savedCart = cartJpa.save(CartJpaEntity.fromDomain(cart));
        List<CartLine> savedLines = insertLines(savedCart.getId(), cart.getLines());
        return savedCart.toDomain(savedLines);
    }

    /**
     * 기존: 라인은 가변(담기·수량변경·제거)이라 전량 교체한다.
     * cart_lines를 cart_id로 모두 삭제한 뒤 현재 라인을 재삽입.
     * (라인의 DB id는 저장마다 새로 부여되지만, 카트 라인 식별 키는 skuId라 무방하다.)
     */
    private Cart update(Cart cart) {
        CartJpaEntity existing = cartJpa.findById(cart.getId())
            .orElseThrow(() -> new IllegalStateException("Cart not found: " + cart.getId()));
        List<CartLine> savedLines = replaceLines(cart.getId(), cart.getLines());
        return existing.toDomain(savedLines);
    }

    private List<CartLine> replaceLines(Long cartId, List<CartLine> lines) {
        cartLineJpa.deleteByCartId(cartId);
        return insertLines(cartId, lines);
    }

    private List<CartLine> insertLines(Long cartId, List<CartLine> lines) {
        if (lines.isEmpty()) {
            return List.of();
        }
        List<CartLineJpaEntity> entities = lines.stream()
            .map(line -> CartLineJpaEntity.fromDomain(cartId, line))
            .toList();
        return cartLineJpa.saveAll(entities).stream()
            .map(CartLineJpaEntity::toDomain)
            .toList();
    }

    @Override
    public Optional<Cart> findByMemberId(Long memberId) {
        return cartJpa.findByMemberId(memberId)
            .map(entity -> entity.toDomain(linesOf(entity.getId())));
    }

    @Override
    public Optional<Cart> findByMemberIdForUpdate(Long memberId) {
        return cartJpa.findByMemberIdForUpdate(memberId)
            .map(entity -> entity.toDomain(linesForUpdateOf(entity.getId())));
    }

    @Override
    public Optional<Cart> findByIdForUpdate(Long cartId) {
        return cartJpa.findByIdForUpdate(cartId)
            .map(entity -> entity.toDomain(linesForUpdateOf(entity.getId())));
    }

    private List<CartLine> linesOf(Long cartId) {
        return cartLineJpa.findByCartId(cartId).stream()
            .map(CartLineJpaEntity::toDomain)
            .toList();
    }

    private List<CartLine> linesForUpdateOf(Long cartId) {
        return cartLineJpa.findByCartIdForUpdate(cartId).stream()
            .map(CartLineJpaEntity::toDomain)
            .toList();
    }
}
