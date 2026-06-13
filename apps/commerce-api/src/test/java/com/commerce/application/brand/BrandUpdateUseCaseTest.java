package com.commerce.application.brand;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
class BrandUpdateUseCaseTest {

    @Mock
    private BrandRepository brandRepository;

    @InjectMocks
    private BrandUpdateUseCase useCase;

    @Test
    @DisplayName("이름과 로고를 수정한다")
    void should_updateBrand_when_validCommand() {
        // given
        Brand brand = Brand.reconstitute(1L, "나이키", "old.jpg", BrandStatus.ACTIVE);
        given(brandRepository.findById(1L)).willReturn(Optional.of(brand));
        given(brandRepository.existsByName("아디다스")).willReturn(false);
        given(brandRepository.save(any(Brand.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        BrandInfo info = useCase.update(new BrandUpdateCommand(1L, " 아디다스 ", "new.jpg"));

        // then
        assertThat(info)
            .satisfies(i -> assertThat(i.name()).isEqualTo("아디다스"))
            .satisfies(i -> assertThat(i.logoUrl()).isEqualTo("new.jpg"));
    }

    @Test
    @DisplayName("다른 이름으로 변경할 때 중복이면 CONFLICT 예외가 발생한다")
    void should_throwConflict_when_newNameDuplicated() {
        // given
        Brand brand = Brand.reconstitute(1L, "나이키", "old.jpg", BrandStatus.ACTIVE);
        given(brandRepository.findById(1L)).willReturn(Optional.of(brand));
        given(brandRepository.existsByName("아디다스")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> useCase.update(new BrandUpdateCommand(1L, "아디다스", "new.jpg")))
            .isInstanceOf(CoreException.class)
            .extracting("errorType").isEqualTo(ErrorType.CONFLICT);
    }
}
