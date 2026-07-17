package com.commerce.domain.cart;

import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

import lombok.Getter;

/**
 * 장바구니 라인. Cart Aggregate 내부의 자식 엔티티(고유 id 보유).
 *
 * <p>주문 라인과 달리 스냅샷하지 않는다 — skuId와 수량만 들고, 가격·상품명·옵션·재고·판매상태는
 * 조회 시점에 Product/Sku에서 live로 다시 읽는다. 라인 식별 키는 skuId다.
 * 수량은 Cart를 통해서만 변경되며(package-private 변경 메서드), public setter는 없다.
 */
@Getter
public class CartLine {

    private final Long id;
    private final Long skuId;
    private int quantity;

    private CartLine(Long id, Long skuId, int quantity) {
        this.id = id;
        this.skuId = skuId;
        this.quantity = quantity;
        validate();
    }

    public static CartLine create(Long skuId, int quantity) {
        return new CartLine(null, skuId, quantity);
    }

    public static CartLine reconstitute(Long id, Long skuId, int quantity) {
        return new CartLine(id, skuId, quantity);
    }

    private void validate() {
        if (skuId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "SKU ID는 필수입니다.");
        }
        requirePositive(quantity);
    }

    /** 같은 SKU를 다시 담을 때 수량을 합산한다(병합). */
    void increaseQuantity(int count) {
        requirePositive(count);
        this.quantity += count;
    }

    /** 수량을 절대값으로 변경한다. */
    void changeQuantity(int newQuantity) {
        requirePositive(newQuantity);
        this.quantity = newQuantity;
    }

    /** 현재 수량보다 작은 구매 완료 수량을 차감한다. 라인 제거 판단은 Cart가 담당한다. */
    void decreaseQuantity(int count) {
        requirePositive(count);
        this.quantity -= count;
    }

    private static void requirePositive(int quantity) {
        if (quantity < 1) {
            throw new CoreException(ErrorType.BAD_REQUEST, "수량은 1개 이상이어야 합니다.");
        }
    }
}
