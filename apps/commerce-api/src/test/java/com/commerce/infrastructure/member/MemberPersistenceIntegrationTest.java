package com.commerce.infrastructure.member;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.commerce.domain.member.Email;
import com.commerce.domain.member.Member;
import com.commerce.domain.member.MemberRepository;
import com.commerce.domain.member.MemberStatus;
import com.commerce.domain.member.Password;
import com.commerce.support.IntegrationTestSupport;

class MemberPersistenceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberJpaRepository memberJpaRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate txTemplate;

    @BeforeEach
    void setUp() {
        txTemplate = new TransactionTemplate(transactionManager);
    }

    @AfterEach
    void tearDown() {
        memberJpaRepository.deleteAll();
    }

    @Test
    @DisplayName("신규 회원 인증 상태 기본값을 저장하고 다시 도메인으로 복원한다")
    void should_persistInitialAuthState_when_save() {
        // given
        Member member = Member.register(new Email("user@example.com"), Password.ofHashed("hashed-password"), "오딘");

        // when
        Member saved = memberRepository.save(member);

        // then
        Member found = memberRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        assertThat(found.getLoginFailureCount()).isZero();
        assertThat(found.getAuthVersion()).isEqualTo(1);
    }

    @Test
    @DisplayName("회원 잠금 상태와 인증 버전 변경이 영속화된다")
    void should_persistAuthStateChange_when_update() {
        // given
        Member saved = memberRepository.save(
            Member.register(new Email("locked@example.com"), Password.ofHashed("hashed-password"), "토르"));

        // when
        txTemplate.executeWithoutResult(s -> {
            Member member = memberRepository.findById(saved.getId()).orElseThrow();
            for (int i = 0; i < 5; i++) {
                member.recordLoginFailure();
            }
            memberRepository.save(member);
        });

        // then
        Member found = memberRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(MemberStatus.LOCKED);
        assertThat(found.getLoginFailureCount()).isEqualTo(5);
        assertThat(found.getAuthVersion()).isEqualTo(2);
    }
}
