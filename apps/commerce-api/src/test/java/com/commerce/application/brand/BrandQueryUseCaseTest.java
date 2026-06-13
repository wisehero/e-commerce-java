package com.commerce.application.brand;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.commerce.domain.brand.Brand;
import com.commerce.domain.brand.BrandRepository;
import com.commerce.domain.brand.BrandStatus;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

@ExtendWith(MockitoExtension.class)
class BrandQueryUseCaseTest {

    @Mock
    private BrandRepository brandRepository;

    @InjectMocks
    private BrandQueryUseCase useCase;

    @Test
    @DisplayName("ACTIVE 브랜드는 단건 조회된다")
    void should_returnBrand_when_active() {
        // given
        given(brandRepository.findById(1L))
            .willReturn(Optional.of(Brand.reconstitute(1L, "나이키", "logo.jpg", BrandStatus.ACTIVE)));

        // when
        BrandInfo info = useCase.getBrand(1L);

        // then
        assertThat(info.name()).isEqualTo("나이키");
    }

    @Test
    @DisplayName("INACTIVE 브랜드는 NOT_FOUND로 숨긴다")
    void should_throwNotFound_when_inactive() {
        // given
        given(brandRepository.findById(1L))
            .willReturn(Optional.of(Brand.reconstitute(1L, "나이키", "logo.jpg", BrandStatus.INACTIVE)));

        // when & then
        assertThatThrownBy(() -> useCase.getBrand(1L))
            .isInstanceOf(CoreException.class)
            .extracting("errorType").isEqualTo(ErrorType.NOT_FOUND);
    }
}
