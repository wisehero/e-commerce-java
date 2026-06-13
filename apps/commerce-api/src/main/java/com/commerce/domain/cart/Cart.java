package com.commerce.domain.cart;

import java.util.ArrayList;
import java.util.List;

import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

import lombok.Getter;

/**
 * 장바구니 Aggregate Root. member당 1개.
 *
 * <p>CartLine을 자식 엔티티로 소유하고 담기·수량변경·제거·비우기를 통해서만 라인을 바꾼다.
 * 같은 SKU를 다시 담으면 수량을 합산한다(병합). 라인 식별 키는 skuId.
 *
 * <p>재고·판매상태 검증은 이 도메인의 책임이 아니다 — Cart는 다른 Aggregate(Sku/Product)를 모른다.
 * 담기 시 재고 검증은 application UseCase가 SkuRepository로 수행한 뒤 이 메서드를 호출한다.
 */
@Getter
public class Cart {

    /** Aggregate 비대 방지: 담을 수 있는 상품 종류(distinct SKU) 상한. */
    public static final int MAX_DISTINCT_LINES = 50;

    private final Long id;
    private final Long memberId;
    private final List<CartLine> lines;

    private Cart(Long id, Long memberId, List<CartLine> lines) {
        this.id = id;
        this.memberId = memberId;
        this.lines = (lines == null) ? new ArrayList<>() : new ArrayList<>(lines);
        validate();
    }

    /** 신규: 빈 장바구니로 생성(첫 담기 시 lazy 생성). */
    public static Cart create(Long memberId) {
        return new Cart(null, memberId, new ArrayList<>());
    }

    public static Cart reconstitute(Long id, Long memberId, List<CartLine> lines) {
        return new Cart(id, memberId, lines);
    }

    private void validate() {
        if (memberId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "장바구니 소유자는 필수입니다.");
        }
    }

    /** 담기: 같은 SKU가 있으면 수량 합산, 없으면 새 라인 추가. */
    public void addItem(Long skuId, int quantity) {
        CartLine existing = findLine(skuId);
        if (existing != null) {
            existing.increaseQuantity(quantity);
            return;
        }
        if (lines.size() >= MAX_DISTINCT_LINES) {
            throw new CoreException(ErrorType.BAD_REQUEST,
                String.format("장바구니에 담을 수 있는 상품은 최대 %d종류입니다.", MAX_DISTINCT_LINES));
        }
        lines.add(CartLine.create(skuId, quantity));
    }

    /** 수량 변경: 절대값으로 변경. 없는 SKU면 NOT_FOUND(제거는 removeItem). */
    public void changeQuantity(Long skuId, int quantity) {
        CartLine line = findLine(skuId);
        if (line == null) {
            throw new CoreException(ErrorType.NOT_FOUND, "장바구니에 없는 상품입니다.");
        }
        line.changeQuantity(quantity);
    }

    public void removeItem(Long skuId) {
        lines.removeIf(line -> line.getSkuId().equals(skuId));
    }

    public void clear() {
        lines.clear();
    }

    public boolean isEmpty() {
        return lines.isEmpty();
    }

    /** 담기 전 재고 검증을 위해 application이 현재 담긴 수량을 조회한다(없으면 0). */
    public int quantityOf(Long skuId) {
        CartLine line = findLine(skuId);
        return (line == null) ? 0 : line.getQuantity();
    }

    public List<CartLine> getLines() {
        return List.copyOf(lines);
    }

    private CartLine findLine(Long skuId) {
        return lines.stream()
            .filter(line -> line.getSkuId().equals(skuId))
            .findFirst()
            .orElse(null);
    }
}
