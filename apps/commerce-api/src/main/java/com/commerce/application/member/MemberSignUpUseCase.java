package com.commerce.application.member;

import org.springframework.stereotype.Service;

import com.commerce.domain.member.Email;
import com.commerce.domain.member.Member;
import com.commerce.domain.member.MemberRepository;
import com.commerce.domain.member.Password;
import com.commerce.domain.member.PasswordHasher;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberSignUpUseCase {

    private final MemberRepository memberRepository;
    private final PasswordHasher passwordHasher;

    @Transactional
    public MemberInfo signUp(MemberSignUpCommand command) {
        Email email = new Email(command.email());

        if (memberRepository.existsByEmail(email)) {
            throw new CoreException(ErrorType.CONFLICT, "이미 가입된 이메일입니다.");
        }
        if (memberRepository.existsByNickname(command.nickname())) {
            throw new CoreException(ErrorType.CONFLICT, "이미 사용 중인 닉네임입니다.");
        }

        Password password = Password.of(command.rawPassword(), passwordHasher);
        Member member = Member.register(email, password, command.nickname());

        Member registeredMember = memberRepository.save(member);

        return MemberInfo.from(registeredMember);
    }
}
