package com.commerce.infrastructure.member;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.commerce.domain.member.Email;
import com.commerce.domain.member.Member;
import com.commerce.domain.member.MemberRepository;
import com.commerce.domain.member.Password;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class MemberRepositoryImpl implements MemberRepository {

    private final MemberJpaRepository jpa;

    @Override
    public Member save(Member member) {
        MemberJpaEntity saved;
        if (member.getId() == null) {
            saved = jpa.save(MemberJpaEntity.fromDomain(member));
        } else {
            MemberJpaEntity existing = jpa.findById(member.getId())
                .orElseThrow(() -> new IllegalStateException("Member not found: " + member.getId()));
            existing.updateFromDomain(member);
            saved = existing;
        }
        return saved.toDomain();
    }

    @Override
    public Optional<Member> findById(Long id) {
        return jpa.findById(id).map(MemberJpaEntity::toDomain);
    }

    @Override
    public Optional<Member> findByEmail(Email email) {
        return jpa.findByEmail(email.value()).map(MemberJpaEntity::toDomain);
    }

    @Override
    public boolean existsByEmail(Email email) {
        return jpa.existsByEmail(email.value());
    }

    @Override
    public boolean existsByNickname(String nickname) {
        return jpa.existsByNickname(nickname);
    }
}
