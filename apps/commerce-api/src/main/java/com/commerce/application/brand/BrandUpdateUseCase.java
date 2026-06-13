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
public class BrandUpdateUseCase {

    private final BrandRepository brandRepository;

    @Transactional
    public BrandInfo update(BrandUpdateCommand command) {
        Brand brand = brandRepository.findById(command.brandId())
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 브랜드입니다."));

        String name = normalizeName(command.name());
        if (!brand.getName().equals(name) && brandRepository.existsByName(name)) {
            throw new CoreException(ErrorType.CONFLICT, "이미 존재하는 브랜드명입니다.");
        }

        brand.rename(name);
        brand.changeLogo(command.logoUrl());

        return BrandInfo.from(brandRepository.save(brand));
    }

    private String normalizeName(String name) {
        return name == null ? null : name.trim();
    }
}
