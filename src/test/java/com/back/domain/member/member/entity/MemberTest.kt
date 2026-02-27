package com.back.domain.member.member.entity

import com.back.domain.member.member.enums.MemberStatus
import com.back.domain.member.member.enums.Role
import com.back.global.exception.ServiceException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class MemberTest {

    @Test
    @DisplayName("관리자 권한이면 ROLE_ADMIN authority를 포함한다.")
    fun t1() {
        val member = Member("admin", "pw", "관리자", Role.ADMIN, null)

        val authorities = member.authorities.map { it.authority }

        assertThat(authorities).contains("ROLE_USER", "ROLE_ADMIN")
    }

    @Test
    @DisplayName("탈퇴 회원은 표시 이름이 '탈퇴한 회원'으로 노출된다.")
    fun t2() {
        val member = Member("u1", "pw", "닉네임", Role.USER, null)
        member.withdraw()

        assertThat(member.name).isEqualTo("탈퇴한 회원")
    }

    @Test
    @DisplayName("잠금 만료 시간이 지난 경우 unlockIfExpired로 잠금 해제된다.")
    fun t3() {
        val member = Member("u1", "pw", "닉네임", Role.USER, null).apply {
            lock()
            increaseFailCount()
            increaseFailCount()
            lockUntil(LocalDateTime.now().minusMinutes(1))
        }

        member.unlockIfExpired()

        assertThat(member.locked).isFalse()
        assertThat(member.lockedUntil).isNull()
        assertThat(member.loginFailCount).isZero()
    }

    @Test
    @DisplayName("다른 회원이 수정 시도하면 예외가 발생한다.")
    fun t4() {
        val target = Member("target", "pw", "대상", Role.USER, null)
        val actor = Member("actor", "pw", "행위자", Role.USER, null)

        assertThatThrownBy { target.checkActorCanModify(actor) }
            .isInstanceOf(ServiceException::class.java)
            .extracting("resultCode")
            .isEqualTo("403-1")
    }

    @Test
    @DisplayName("WITHDRAWN 상태에서 ACTIVE로 변경하려고 하면 실패한다.")
    fun t5() {
        val member = Member("u1", "pw", "닉네임", Role.USER, null)
        member.withdraw()

        assertThatThrownBy { member.changeStatus(MemberStatus.ACTIVE) }
            .isInstanceOf(ServiceException::class.java)
            .extracting("resultCode")
            .isEqualTo("400-4")
    }

    @Test
    @DisplayName("suspend/release 호출 시 상태와 시각 필드가 갱신된다.")
    fun t6() {
        val member = Member("u1", "pw", "닉네임", Role.USER, null)

        member.suspend()
        assertThat(member.status).isEqualTo(MemberStatus.SUSPENDED)
        assertThat(member.suspendAt).isNotNull()

        member.release()
        assertThat(member.status).isEqualTo(MemberStatus.ACTIVE)
        assertThat(member.suspendAt).isNull()
    }
}
