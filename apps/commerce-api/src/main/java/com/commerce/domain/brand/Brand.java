package com.commerce.domain.brand;

import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

import lombok.Getter;

@Getter
public class Brand {

    private static final int NAME_MAX = 50;

    private Long id;
    private String name;
    private String logoUrl;
    private BrandStatus status;

    private Brand(Long id, String name, String logoUrl, BrandStatus status) {
        this.id = id;
        this.name = name;
        this.logoUrl = logoUrl;
        this.status = status;
        validate();
    }

    public static Brand register(String name, String logoUrl) {
        return new Brand(null, name, logoUrl, BrandStatus.ACTIVE);
    }

    public static Brand reconstitute(Long id, String name, String logoUrl, BrandStatus status) {
        return new Brand(id, name, logoUrl, status);
    }

    private void validate() {
        validateName(name);
        if (status == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "브랜드 상태는 필수입니다.");
        }
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank() || name.length() > NAME_MAX) {
            throw new CoreException(ErrorType.BAD_REQUEST,
                String.format("브랜드명은 1~%d자여야 합니다.", NAME_MAX));
        }
    }

    public boolean isVisible() {
        return status == BrandStatus.ACTIVE;
    }

    public void rename(String newName) {
        validateName(newName);
        this.name = newName;
    }

    public void changeLogo(String newLogoUrl) {
        this.logoUrl = newLogoUrl;
    }

    public void activate() {
        if (status == BrandStatus.ACTIVE) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이미 활성화된 브랜드입니다.");
        }
        this.status = BrandStatus.ACTIVE;
    }

    public void deactivate() {
        if (status == BrandStatus.INACTIVE) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이미 비활성화된 브랜드입니다.");
        }
        this.status = BrandStatus.INACTIVE;
    }
}
