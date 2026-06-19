package com.commerce.infrastructure.member;

import com.commerce.domain.member.Email;
import com.commerce.domain.member.Member;
import com.commerce.domain.member.MemberGrade;
import com.commerce.domain.member.MemberRole;
import com.commerce.domain.member.Password;
import com.commerce.infrastructure.jpa.BaseJpaEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

@Entity
@Table(
    name = "members", uniqueConstraints = {
    @UniqueConstraint(name = "uk_members_email", columnNames = "email"),
    @UniqueConstraint(name = "uk_members_nickname", columnNames = "nickname")
})
@Getter
public class MemberJpaEntity extends BaseJpaEntity {

    @Column(name = "email", nullable = false, length = 320)
    private String email;

    @Column(name = "password", nullable = false, length = 100)
    private String password;

    @Column(name = "nickname", nullable = false, length = 20)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private MemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "grade", nullable = false, length = 20)
    private MemberGrade grade;

    protected MemberJpaEntity() {
    }

    private MemberJpaEntity(String email, String password, String nickname, MemberRole role, MemberGrade grade) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.role = role;
        this.grade = grade;
    }

    /** 도메인 → 신규 엔티티 */
    public static MemberJpaEntity fromDomain(Member member) {
        return new MemberJpaEntity(
            member.getEmail().value(),
            member.getPassword().hashedValue(),
            member.getNickname(),
            member.getRole(),
            member.getGrade()
        );
    }

    /** 엔티티 → 도메인 */
    public Member toDomain() {
        return Member.reconstitute(
            this.getId(),
            new Email(email),
            Password.ofHashed(password),
            nickname,
            role,
            grade
        );
    }

    /** 영속 상태 엔티티에 도메인 상태를 반영 (Dirty Checking) */
    public void updateFromDomain(Member member) {
        this.email = member.getEmail().value();
        this.password = member.getPassword().hashedValue();
        this.nickname = member.getNickname();
        this.role = member.getRole();
        this.grade = member.getGrade();
    }

}
