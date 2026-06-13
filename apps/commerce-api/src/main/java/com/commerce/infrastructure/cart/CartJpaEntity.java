package com.commerce.infrastructure.cart;

import java.util.List;

import com.commerce.domain.cart.Cart;
import com.commerce.domain.cart.CartLine;
import com.commerce.infrastructure.jpa.BaseJpaEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 장바구니 루트 매핑. member당 1개(member_id 유니크).
 * 라인은 별도 테이블(cart_lines)이라 여기 보유하지 않고 CartRepositoryImpl이 두 테이블을 조립한다.
 * (JPA 연관 어노테이션 미사용)
 */
@Entity
@Table(name = "carts", uniqueConstraints = {
    @UniqueConstraint(name = "uk_carts_member_id", columnNames = "member_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CartJpaEntity extends BaseJpaEntity {

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    private CartJpaEntity(Long memberId) {
        this.memberId = memberId;
    }

    public static CartJpaEntity fromDomain(Cart cart) {
        return new CartJpaEntity(cart.getMemberId());
    }

    /** 라인은 별도 테이블에서 로드해 넘겨준다. */
    public Cart toDomain(List<CartLine> lines) {
        return Cart.reconstitute(getId(), memberId, lines);
    }
}
