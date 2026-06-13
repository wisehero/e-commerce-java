package com.commerce.domain.product;

import java.util.List;

import com.commerce.domain.shared.Money;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

import lombok.Getter;

@Getter
public class Sku {

    private Long id;
    private Long productId;
    private List<OptionValue> optionValues;
    private Money originalPrice;
    private Money salePrice;
    private Stock stock;

    private Sku(Long id, Long productId, List<OptionValue> optionValues,
        Money originalPrice, Money salePrice, Stock stock) {
        this.id = id;
        this.productId = productId;
        this.optionValues = (optionValues == null) ? null : List.copyOf(optionValues);
        this.originalPrice = originalPrice;
        this.salePrice = salePrice;
        this.stock = stock;
        validate();
    }

    /** 신규: 할인 없이 정가로 시작 (salePrice = originalPrice) */
    public static Sku create(Long productId, List<OptionValue> optionValues,
        Money originalPrice, Stock stock) {
        return new Sku(null, productId, optionValues, originalPrice, originalPrice, stock);
    }

    public static Sku reconstitute(Long id, Long productId, List<OptionValue> optionValues,
        Money originalPrice, Money salePrice, Stock stock) {
        return new Sku(id, productId, optionValues, originalPrice, salePrice, stock);
    }

    private void validate() {
        if (productId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품 ID는 필수입니다.");
        }
        validateOptions(optionValues);
        if (originalPrice == null || salePrice == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "가격은 필수입니다.");
        }
        if (salePrice.isGreaterThan(originalPrice)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "할인가는 정가보다 클 수 없습니다.");
        }
        if (stock == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "재고는 필수입니다.");
        }
    }

    private static void validateOptions(List<OptionValue> optionValues) {
        if (optionValues == null || optionValues.isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "옵션은 최소 1개여야 합니다.");
        }
        long distinctNames = optionValues.stream().map(OptionValue::name).distinct().count();
        if (distinctNames != optionValues.size()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "옵션명은 중복될 수 없습니다.");
        }
    }

    public void applyDiscount(Money newSalePrice) {
        if (newSalePrice == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "할인가는 필수입니다.");
        }
        if (newSalePrice.isGreaterThan(originalPrice)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "할인가는 정가보다 클 수 없습니다.");
        }
        this.salePrice = newSalePrice;
    }

    public void clearDiscount() {
        this.salePrice = this.originalPrice;
    }

    public void changePrice(Money newOriginalPrice) {
        if (newOriginalPrice == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "정가는 필수입니다.");
        }
        this.originalPrice = newOriginalPrice;
        this.salePrice = newOriginalPrice;   // 정가 변경 시 할인 초기화
    }

    public boolean isDiscounted() {
        return originalPrice.isGreaterThan(salePrice);
    }

    public void restock(int count) {
        this.stock = this.stock.increase(count);
    }

    public void decreaseStock(int count) {
        this.stock = this.stock.decrease(count);
    }
}
