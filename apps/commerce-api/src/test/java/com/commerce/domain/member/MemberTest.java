package com.commerce.domain.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

class MemberTest {

    private static final PasswordHasher HASHER = new FakePasswordHasher();
    private static final Email VALID_EMAIL = new Email("user@example.com");
    private static final Password VALID_PASSWORD = Password.of("password123", HASHER);
    private static final String VALID_NICKNAME = "오딘";
    private static final MemberGrade VALID_GRADE = MemberGrade.GOLD;

    @Nested
    @DisplayName("register (신규 가입)")
    class Register {

        @Test
        @DisplayName("id 없이 USER 권한, BRONZE 등급, ACTIVE 상태로 생성된다")
        void should_createWithoutId_andUserRole_when_register() {
            // when
            Member member = Member.register(VALID_EMAIL, VALID_PASSWORD, VALID_NICKNAME);

            // then
            assertThat(member)
                .satisfies(m -> assertThat(m.getId()).isNull())
                .satisfies(m -> assertThat(m.getEmail()).isEqualTo(VALID_EMAIL))
                .satisfies(m -> assertThat(m.getPassword()).isEqualTo(VALID_PASSWORD))
                .satisfies(m -> assertThat(m.getNickname()).isEqualTo(VALID_NICKNAME))
                .satisfies(m -> assertThat(m.getRole()).isEqualTo(MemberRole.USER))
                .satisfies(m -> assertThat(m.getGrade()).isEqualTo(MemberGrade.BRONZE))
                .satisfies(m -> assertThat(m.getStatus()).isEqualTo(MemberStatus.ACTIVE))
                .satisfies(m -> assertThat(m.getLoginFailureCount()).isZero())
                .satisfies(m -> assertThat(m.getAuthVersion()).isEqualTo(1));
        }

        @Test
        @DisplayName("이메일이 null이면 BAD_REQUEST 예외가 발생한다")
        void should_throwException_when_register_emailNull() {
            // when & then
            assertThatThrownBy(() -> Member.register(null, VALID_PASSWORD, VALID_NICKNAME))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }

        @Test
        @DisplayName("비밀번호가 null이면 BAD_REQUEST 예외가 발생한다")
        void should_throwException_when_register_passwordNull() {
            // when & then
            assertThatThrownBy(() -> Member.register(VALID_EMAIL, null, VALID_NICKNAME))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"a", "012345678901234567890"})
        @DisplayName("닉네임이 2~20자 범위를 벗어나면 BAD_REQUEST 예외가 발생한다")
        void should_throwException_when_register_nicknameOutOfRange(String nickname) {
            // when & then
            assertThatThrownBy(() -> Member.register(VALID_EMAIL, VALID_PASSWORD, nickname))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }

        @ParameterizedTest
        @ValueSource(strings = {"ab", "01234567890123456789"})
        @DisplayName("닉네임 경계값(2자, 20자)은 허용된다")
        void should_acceptBoundary_when_nickname2or20chars(String nickname) {
            // when
            Member member = Member.register(VALID_EMAIL, VALID_PASSWORD, nickname);

            // then
            assertThat(member.getNickname()).isEqualTo(nickname);
        }
    }

    @Nested
    @DisplayName("reconstitute (영속 복원)")
    class Reconstitute {

        @Test
        @DisplayName("id, role, grade, status, loginFailureCount, authVersion을 그대로 복원한다")
        void should_reconstitute_when_validArgs() {
            // when
            Member member = Member.reconstitute(
                42L, VALID_EMAIL, VALID_PASSWORD, VALID_NICKNAME, MemberRole.ADMIN, MemberGrade.VIP,
                MemberStatus.SUSPENDED, 3, 7);

            // then
            assertThat(member)
                .satisfies(m -> assertThat(m.getId()).isEqualTo(42L))
                .satisfies(m -> assertThat(m.getRole()).isEqualTo(MemberRole.ADMIN))
                .satisfies(m -> assertThat(m.getGrade()).isEqualTo(MemberGrade.VIP))
                .satisfies(m -> assertThat(m.getStatus()).isEqualTo(MemberStatus.SUSPENDED))
                .satisfies(m -> assertThat(m.getLoginFailureCount()).isEqualTo(3))
                .satisfies(m -> assertThat(m.getAuthVersion()).isEqualTo(7));
        }

        @Test
        @DisplayName("권한이 null이면 BAD_REQUEST 예외가 발생한다")
        void should_throwException_when_reconstitute_roleNull() {
            // when & then
            assertThatThrownBy(() -> Member.reconstitute(
                    1L, VALID_EMAIL, VALID_PASSWORD, VALID_NICKNAME, null, VALID_GRADE))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }

        @Test
        @DisplayName("등급이 null이면 BAD_REQUEST 예외가 발생한다")
        void should_throwException_when_reconstitute_gradeNull() {
            // when & then
            assertThatThrownBy(() -> Member.reconstitute(
                    1L, VALID_EMAIL, VALID_PASSWORD, VALID_NICKNAME, MemberRole.USER, null))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }

        @Test
        @DisplayName("상태가 null이면 BAD_REQUEST 예외가 발생한다")
        void should_throwException_when_reconstitute_statusNull() {
            // when & then
            assertThatThrownBy(() -> Member.reconstitute(
                    1L, VALID_EMAIL, VALID_PASSWORD, VALID_NICKNAME, MemberRole.USER, VALID_GRADE, null, 0, 1))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }

        @Test
        @DisplayName("로그인 실패 횟수가 음수이면 BAD_REQUEST 예외가 발생한다")
        void should_throwException_when_reconstitute_loginFailureCountNegative() {
            // when & then
            assertThatThrownBy(() -> Member.reconstitute(
                    1L, VALID_EMAIL, VALID_PASSWORD, VALID_NICKNAME, MemberRole.USER, VALID_GRADE,
                    MemberStatus.ACTIVE, -1, 1))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }

        @Test
        @DisplayName("인증 버전이 1보다 작으면 BAD_REQUEST 예외가 발생한다")
        void should_throwException_when_reconstitute_authVersionLessThanOne() {
            // when & then
            assertThatThrownBy(() -> Member.reconstitute(
                    1L, VALID_EMAIL, VALID_PASSWORD, VALID_NICKNAME, MemberRole.USER, VALID_GRADE,
                    MemberStatus.ACTIVE, 0, 0))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("권한 확인")
    class Role {

        @Test
        @DisplayName("ADMIN 권한이면 isAdmin은 true다")
        void should_returnTrue_when_isAdmin_andRoleAdmin() {
            // given
            Member admin = Member.reconstitute(
                1L, VALID_EMAIL, VALID_PASSWORD, VALID_NICKNAME, MemberRole.ADMIN, VALID_GRADE);

            // when & then
            assertThat(admin.isAdmin()).isTrue();
        }

        @Test
        @DisplayName("USER 권한이면 isAdmin은 false다")
        void should_returnFalse_when_isAdmin_andRoleUser() {
            // given
            Member user = Member.register(VALID_EMAIL, VALID_PASSWORD, VALID_NICKNAME);

            // when & then
            assertThat(user.isAdmin()).isFalse();
        }

        @Test
        @DisplayName("hasRole은 같은 권한일 때만 true다")
        void should_returnTrueOrFalse_when_hasRole() {
            // given
            Member user = Member.register(VALID_EMAIL, VALID_PASSWORD, VALID_NICKNAME);

            // when & then
            assertThat(user.hasRole(MemberRole.USER)).isTrue();
            assertThat(user.hasRole(MemberRole.ADMIN)).isFalse();
        }
    }

    @Nested
    @DisplayName("matchPassword")
    class MatchPassword {

        @Test
        @DisplayName("Password에 위임해 평문 일치 여부를 반환한다")
        void should_delegateToPassword_when_matchPassword() {
            // given
            Member member = Member.register(VALID_EMAIL, VALID_PASSWORD, VALID_NICKNAME);

            // when & then
            assertThat(member.matchPassword("password123", HASHER)).isTrue();
            assertThat(member.matchPassword("wrong-password", HASHER)).isFalse();
        }
    }

    @Nested
    @DisplayName("로그인 상태")
    class LoginState {

        @Test
        @DisplayName("로그인 실패 4회까지는 ACTIVE 상태를 유지한다")
        void should_keepActive_when_loginFailureCountLessThanFive() {
            // given
            Member member = Member.register(VALID_EMAIL, VALID_PASSWORD, VALID_NICKNAME);

            // when
            for (int i = 0; i < 4; i++) {
                member.recordLoginFailure();
            }

            // then
            assertThat(member.getLoginFailureCount()).isEqualTo(4);
            assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
            assertThat(member.getAuthVersion()).isEqualTo(1);
        }

        @Test
        @DisplayName("로그인 실패 5회에 도달하면 LOCKED 상태가 되고 authVersion이 증가한다")
        void should_lockAndIncreaseAuthVersion_when_loginFailureCountReachesFive() {
            // given
            Member member = Member.register(VALID_EMAIL, VALID_PASSWORD, VALID_NICKNAME);

            // when
            for (int i = 0; i < 5; i++) {
                member.recordLoginFailure();
            }

            // then
            assertThat(member.getLoginFailureCount()).isEqualTo(5);
            assertThat(member.getStatus()).isEqualTo(MemberStatus.LOCKED);
            assertThat(member.getAuthVersion()).isEqualTo(2);
        }

        @Test
        @DisplayName("로그인 실패 횟수를 초기화한다")
        void should_resetLoginFailures() {
            // given
            Member member = Member.register(VALID_EMAIL, VALID_PASSWORD, VALID_NICKNAME);
            member.recordLoginFailure();
            member.recordLoginFailure();

            // when
            member.resetLoginFailures();

            // then
            assertThat(member.getLoginFailureCount()).isZero();
            assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
            assertThat(member.getAuthVersion()).isEqualTo(1);
        }

        @ParameterizedTest
        @ValueSource(strings = {"LOCKED", "SUSPENDED", "WITHDRAWN"})
        @DisplayName("LOCKED, SUSPENDED, WITHDRAWN 상태는 로그인할 수 없다")
        void should_throwException_when_loginNotAllowed(MemberStatus status) {
            // given
            Member member = Member.reconstitute(1L, VALID_EMAIL, VALID_PASSWORD, VALID_NICKNAME,
                MemberRole.USER, VALID_GRADE, status, 0, 2);

            // when & then
            assertThatThrownBy(member::ensureLoginAllowed)
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }

        @Test
        @DisplayName("ACTIVE 상태는 로그인할 수 있다")
        void should_notThrowException_when_active() {
            // given
            Member member = Member.register(VALID_EMAIL, VALID_PASSWORD, VALID_NICKNAME);

            // when & then
            member.ensureLoginAllowed();
        }
    }

    @Nested
    @DisplayName("상태 변경")
    class StatusChange {

        @Test
        @DisplayName("정지하면 SUSPENDED 상태가 되고 authVersion이 증가한다")
        void should_suspendAndIncreaseAuthVersion() {
            // given
            Member member = Member.register(VALID_EMAIL, VALID_PASSWORD, VALID_NICKNAME);

            // when
            member.suspend();

            // then
            assertThat(member.getStatus()).isEqualTo(MemberStatus.SUSPENDED);
            assertThat(member.getAuthVersion()).isEqualTo(2);
        }

        @Test
        @DisplayName("탈퇴하면 WITHDRAWN 상태가 되고 authVersion이 증가한다")
        void should_withdrawAndIncreaseAuthVersion() {
            // given
            Member member = Member.register(VALID_EMAIL, VALID_PASSWORD, VALID_NICKNAME);

            // when
            member.withdraw();

            // then
            assertThat(member.getStatus()).isEqualTo(MemberStatus.WITHDRAWN);
            assertThat(member.getAuthVersion()).isEqualTo(2);
        }

        @Test
        @DisplayName("잠금 해제하면 ACTIVE 상태가 되고 실패 횟수가 초기화되며 authVersion이 증가한다")
        void should_unlockAndResetLoginFailuresAndIncreaseAuthVersion() {
            // given
            Member member = Member.reconstitute(1L, VALID_EMAIL, VALID_PASSWORD, VALID_NICKNAME,
                MemberRole.USER, VALID_GRADE, MemberStatus.LOCKED, 5, 2);

            // when
            member.unlock();

            // then
            assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
            assertThat(member.getLoginFailureCount()).isZero();
            assertThat(member.getAuthVersion()).isEqualTo(3);
        }

        @Test
        @DisplayName("authVersion을 명시적으로 증가시킨다")
        void should_increaseAuthVersion() {
            // given
            Member member = Member.register(VALID_EMAIL, VALID_PASSWORD, VALID_NICKNAME);

            // when
            member.increaseAuthVersion();

            // then
            assertThat(member.getAuthVersion()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("changeNickname")
    class ChangeNickname {

        @Test
        @DisplayName("유효한 닉네임이면 변경된다")
        void should_changeNickname_when_valid() {
            // given
            Member member = Member.register(VALID_EMAIL, VALID_PASSWORD, VALID_NICKNAME);

            // when
            member.changeNickname("토르");

            // then
            assertThat(member.getNickname()).isEqualTo("토르");
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"a", "012345678901234567890"})
        @DisplayName("닉네임이 2~20자 범위를 벗어나면 BAD_REQUEST 예외가 발생한다")
        void should_throwException_when_changeNickname_outOfRange(String nickname) {
            // given
            Member member = Member.register(VALID_EMAIL, VALID_PASSWORD, VALID_NICKNAME);

            // when & then
            assertThatThrownBy(() -> member.changeNickname(nickname))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("changeGrade")
    class ChangeGrade {

        @Test
        @DisplayName("유효한 등급이면 변경된다")
        void should_changeGrade_when_valid() {
            // given
            Member member = Member.register(VALID_EMAIL, VALID_PASSWORD, VALID_NICKNAME);

            // when
            member.changeGrade(MemberGrade.VIP);

            // then
            assertThat(member.getGrade()).isEqualTo(MemberGrade.VIP);
        }

        @Test
        @DisplayName("등급이 null이면 BAD_REQUEST 예외가 발생한다")
        void should_throwException_when_changeGrade_null() {
            // given
            Member member = Member.register(VALID_EMAIL, VALID_PASSWORD, VALID_NICKNAME);

            // when & then
            assertThatThrownBy(() -> member.changeGrade(null))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    /** 테스트용 결정론적 해셔. "hashed:" 접두사 방식. */
    private static class FakePasswordHasher implements PasswordHasher {
        @Override
        public String hash(String rawPassword) {
            return "hashed:" + rawPassword;
        }

        @Override
        public boolean matches(String rawPassword, String hashedPassword) {
            return hashedPassword.equals("hashed:" + rawPassword);
        }
    }
}
