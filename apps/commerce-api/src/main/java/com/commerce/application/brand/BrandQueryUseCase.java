package com.commerce.application.brand;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.commerce.domain.brand.Brand;
import com.commerce.domain.brand.BrandRepository;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BrandQueryUseCase {

    private final BrandRepository brandRepository;

    @Transactional(readOnly = true)
    public BrandInfo getBrand(Long brandId) {
        Brand brand = brandRepository.findById(brandId)
            .filter(Brand::isVisible)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 브랜드입니다."));

        return BrandInfo.from(brand);
    }
}
