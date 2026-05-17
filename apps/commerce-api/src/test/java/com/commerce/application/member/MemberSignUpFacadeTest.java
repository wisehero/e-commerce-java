package com.commerce.application.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.commerce.domain.member.Email;
import com.commerce.domain.member.Member;
import com.commerce.domain.member.MemberRepository;
import com.commerce.domain.member.MemberRole;
import com.commerce.domain.member.PasswordHasher;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

@ExtendWith(MockitoExtension.class)
class MemberSignUpFacadeTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordHasher passwordHasher;

    @InjectMocks
    private MemberSignUpFacade facade;

    @Nested
    @DisplayName("회원가입")
    class SignUp {

        private final MemberSignUpCommand command =
            new MemberSignUpCommand("user@example.com", "password123", "오딘");

        @Test
        @DisplayName("유효한 명령으로 회원가입하면 MemberInfo를 반환한다")
        void should_returnMemberInfo_when_signUpWithValidCommand() {
            // given
            given(memberRepository.existsByEmail(new Email("user@example.com"))).willReturn(false);
            given(memberRepository.existsByNickname("오딘")).willReturn(false);
            given(passwordHasher.hash("password123")).willReturn("hashed:password123");
            given(memberRepository.save(any(Member.class))).willAnswer(invocation -> {
                Member m = invocation.getArgument(0);
                return Member.reconstitute(
                    42L, m.getEmail(), m.getPassword(), m.getNickname(), m.getRole());
            });

            // when
            MemberInfo info = facade.signUp(command);

            // then
            assertThat(info).usingRecursiveComparison()
                .isEqualTo(new MemberInfo(42L, "user@example.com", "오딘", MemberRole.USER));
        }

        @Test
        @DisplayName("이미 가입된 이메일이면 CONFLICT 예외가 발생한다")
        void should_throwConflict_when_emailAlreadyExists() {
            // given
            given(memberRepository.existsByEmail(new Email("user@example.com"))).willReturn(true);

            // when & then
            assertThatThrownBy(() -> facade.signUp(command))
                .isInstanceOf(CoreException.class)
                .hasMessage("이미 가입된 이메일입니다.")
                .extracting("errorType").isEqualTo(ErrorType.CONFLICT);
        }

        @Test
        @DisplayName("이미 사용중인 닉네임이면 CONFLICT 예외가 발생한다")
        void should_throwConflict_when_nicknameAlreadyExists() {
            // given
            given(memberRepository.existsByEmail(new Email("user@example.com"))).willReturn(false);
            given(memberRepository.existsByNickname("오딘")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> facade.signUp(command))
                .isInstanceOf(CoreException.class)
                .hasMessage("이미 사용 중인 닉네임입니다.")
                .extracting("errorType").isEqualTo(ErrorType.CONFLICT);
        }

        @Test
        @DisplayName("이메일 형식이 잘못되면 BAD_REQUEST 예외가 발생한다")
        void should_throwBadRequest_when_emailFormatInvalid() {
            // given
            MemberSignUpCommand invalid =
                new MemberSignUpCommand("abc", "password123", "오딘");

            // when
            assertThatThrownBy(() -> facade.signUp(invalid))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);

            // then
            then(memberRepository).should(never()).existsByEmail(any());
            then(memberRepository).should(never()).existsByNickname(any());
            then(memberRepository).should(never()).save(any(Member.class));
        }

        @Test
        @DisplayName("회원 저장 시 비밀번호가 해싱되어 전달된다")
        void should_hashPasswordBeforeSave() {
            // given
            given(memberRepository.existsByEmail(any())).willReturn(false);
            given(memberRepository.existsByNickname("오딘")).willReturn(false);
            given(passwordHasher.hash("password123")).willReturn("hashed:password123");
            given(memberRepository.save(any(Member.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

            // when
            facade.signUp(command);

            // then
            ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
            then(memberRepository).should().save(captor.capture());
            assertThat(captor.getValue().getPassword().hashedValue())
                .isEqualTo("hashed:password123");
        }

        @Test
        @DisplayName("이메일 중복이면 닉네임 중복 검사는 수행하지 않는다")
        void should_skipNicknameCheck_when_emailAlreadyExists() {
            // given
            given(memberRepository.existsByEmail(any())).willReturn(true);

            // when
            assertThatThrownBy(() -> facade.signUp(command))
                .isInstanceOf(CoreException.class);

            // then
            then(memberRepository).should(never()).existsByNickname(any());
            then(memberRepository).should(never()).save(any(Member.class));
        }
    }
}
