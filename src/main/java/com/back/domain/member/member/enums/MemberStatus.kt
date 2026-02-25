package com.back.domain.member.member.enums

enum class MemberStatus {
    ACTIVE,  // 정상
    SUSPENDED,  // 일시 정지
    WITHDRAWN,  // 회원 탈퇴
    BANNED; // 영구 정지

    fun canTransitionTo(target: MemberStatus): Boolean =
        when (this) {
            MemberStatus.ACTIVE    -> target in setOf(SUSPENDED, BANNED, WITHDRAWN)
            MemberStatus.SUSPENDED -> target in setOf(ACTIVE, BANNED, WITHDRAWN)
            MemberStatus.BANNED    -> target in setOf(ACTIVE, WITHDRAWN)
            MemberStatus.WITHDRAWN -> false
        }
}
