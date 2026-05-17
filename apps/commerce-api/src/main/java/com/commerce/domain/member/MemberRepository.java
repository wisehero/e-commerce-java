package com.commerce.domain.member;

import java.util.Optional;

public interface MemberRepository {

    Member save(Member member);

    Optional<Member> findById(Long id);

    Optional<Member> findByEmail(Email email);

    boolean existsByEmail(Email email);

    boolean existsByNickname(String nickname);
}
