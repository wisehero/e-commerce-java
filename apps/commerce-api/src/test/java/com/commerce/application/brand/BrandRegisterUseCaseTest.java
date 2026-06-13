package com.commerce.application.brand;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.commerce.domain.brand.Brand;
import com.commerce.domain.brand.BrandRepository;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

@ExtendWith(MockitoExtension.class)
class BrandRegisterUseCaseTest {

    @Mock
    private BrandRepository brandRepository;

    @InjectMocks
    private BrandRegisterUseCase useCase;

    @Test
    @DisplayName("브랜드명을 trim 후 중복 검증하고 ACTIVE 브랜드를 등록한다")
    void should_registerBrand_when_validCommand() {
        // given
        BrandRegisterCommand command = new BrandRegisterCommand("  나이키  ", "logo.jpg");
        given(brandRepository.existsByName("나이키")).willReturn(false);
        given(brandRepository.save(any(Brand.class))).willAnswer(inv -> {
            Brand brand = inv.getArgument(0);
            return Brand.reconstitute(1L, brand.getName(), brand.getLogoUrl(), brand.getStatus());
        });

        // when
        BrandInfo info = useCase.register(command);

        // then
        assertThat(info)
            .satisfies(i -> assertThat(i.id()).isEqualTo(1L))
            .satisfies(i -> assertThat(i.name()).isEqualTo("나이키"))
            .satisfies(i -> assertThat(i.status()).isEqualTo("ACTIVE"));
    }

    @Test
    @DisplayName("브랜드명이 중복되면 CONFLICT 예외가 발생하고 저장하지 않는다")
    void should_throwConflict_when_nameDuplicated() {
        // given
        BrandRegisterCommand command = new BrandRegisterCommand("나이키", "logo.jpg");
        given(brandRepository.existsByName("나이키")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> useCase.register(command))
            .isInstanceOf(CoreException.class)
            .extracting("errorType").isEqualTo(ErrorType.CONFLICT);
        then(brandRepository).should(never()).save(any());
    }
}
