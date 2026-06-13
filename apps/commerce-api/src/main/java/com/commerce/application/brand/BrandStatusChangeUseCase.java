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
public class BrandStatusChangeUseCase {

    private final BrandRepository brandRepository;

    @Transactional
    public void activate(Long brandId) {
        Brand brand = findBrand(brandId);
        brand.activate();
        brandRepository.save(brand);
    }

    @Transactional
    public void deactivate(Long brandId) {
        Brand brand = findBrand(brandId);
        brand.deactivate();
        brandRepository.save(brand);
    }

    private Brand findBrand(Long brandId) {
        return brandRepository.findById(brandId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 브랜드입니다."));
    }
}
