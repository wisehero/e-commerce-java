package com.commerce.domain.product;

import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

import lombok.Getter;

@Getter
public class Product {

    private static final int NAME_MAX = 100;

    private Long id;
    private String name;
    private String description;
    private Long categoryId;
    private Long brandId;
    private String imageUrl;
    private ProductStatus status;

    private Product(Long id, String name, String description, Long categoryId, Long brandId, String imageUrl,
        ProductStatus status) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.categoryId = categoryId;
        this.brandId = brandId;
        this.imageUrl = imageUrl;
        this.status = status;
        validate();
    }

    public static Product register(String name, String description, Long categoryId, Long brandId, String imageUrl) {
        return new Product(null, name, description, categoryId, brandId, imageUrl, ProductStatus.ON_SALE);
    }

    public static Product reconstitute(Long id, String name, String description, Long categoryId,
        Long brandId, String imageUrl, ProductStatus status) {
        return new Product(id, name, description, categoryId, brandId, imageUrl, status);
    }

    private void validate() {
        validateName(name);
        if (categoryId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "카테고리는 필수입니다.");
        }
        if (status == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품 상태는 필수입니다.");
        }
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank() || name.length() > NAME_MAX) {
            throw new CoreException(ErrorType.BAD_REQUEST,
                String.format("상품명은 1~%d자여야 합니다.", NAME_MAX));
        }
    }

    public boolean isVisible() {
        return status == ProductStatus.ON_SALE;
    }

    public void suspend() {
        if (status != ProductStatus.ON_SALE) {
            throw new CoreException(ErrorType.BAD_REQUEST, "판매중인 상품만 판매중지할 수 있습니다.");
        }
        this.status = ProductStatus.SUSPENDED;
    }

    public void resume() {
        if (status != ProductStatus.SUSPENDED) {
            throw new CoreException(ErrorType.BAD_REQUEST, "일시중지된 상품만 판매를 재개할 수 있습니다.");
        }
        this.status = ProductStatus.ON_SALE;
    }

    public void discontinue() {
        if (status == ProductStatus.DISCONTINUED) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이미 단종된 상품입니다.");
        }
        this.status = ProductStatus.DISCONTINUED;
    }
}
