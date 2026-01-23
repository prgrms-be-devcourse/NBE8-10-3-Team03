package com.back.domain.member.member.enums;

public enum MemberStatus {
    ACTIVE,        // 정상
    SUSPENDED,     // 일시 정지
    WITHDRAWN,     // 회원 탈퇴
    BANNED;         // 영구 정지

    public boolean canTransitionTo(MemberStatus target) {
        return switch (this) {
            case ACTIVE ->
                    target == SUSPENDED || target == BANNED || target == WITHDRAWN;

            case SUSPENDED ->
                    target == ACTIVE || target == BANNED || target == WITHDRAWN;

            case BANNED ->
                    target == ACTIVE || target == WITHDRAWN;

            case WITHDRAWN -> false; // 여기서 끝
        };
    }
}
