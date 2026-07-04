package com.commerce.domain.member;

public enum MemberStatus {
    ACTIVE,
    LOCKED,
    SUSPENDED,
    WITHDRAWN;

    public boolean canLogin() {
        return this == ACTIVE;
    }
}
