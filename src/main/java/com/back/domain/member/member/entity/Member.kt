package com.back.domain.member.member.entity

import com.back.domain.member.member.enums.MemberStatus
import com.back.domain.member.member.enums.Role
import com.back.domain.member.reputation.entity.Report
import com.back.domain.member.reputation.entity.Reputation
import com.back.domain.member.reputation.entity.ReputationEvent
import com.back.domain.member.review.entity.Review
import com.back.global.exception.ServiceException
import com.back.global.jpa.entity.BaseEntity
import jakarta.persistence.*
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "users")
class Member(
    @Column(unique = true)
    var username: String,

    var password: String? = null,

    @Column(unique = true)
    var nickname: String,

    @Enumerated(EnumType.STRING)
    var role: Role?,

    var profileImgUrl: String?,

) : BaseEntity() {

    // ========================
    // 필드
    // ========================

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: MemberStatus = MemberStatus.ACTIVE

    @Column(unique = true)
    var apiKey: String? = UUID.randomUUID().toString()

    @OneToOne(mappedBy = "member", fetch = FetchType.LAZY)
    var reputation: Reputation? = null

    @OneToMany(mappedBy = "target", fetch = FetchType.LAZY)
    var targetEvents: MutableList<ReputationEvent?> = mutableListOf()

    @OneToMany(mappedBy = "target", fetch = FetchType.LAZY)
    var targetReports: MutableList<Report?> = mutableListOf()

    @OneToMany(mappedBy = "reporter", fetch = FetchType.LAZY)
    var reporterReports: MutableList<Report?> = mutableListOf()

    @OneToMany(mappedBy = "member", fetch = FetchType.LAZY)
    var reviews: MutableList<Review> = mutableListOf()

    @Column(nullable = true)
    var suspendAt: LocalDateTime? = null

    @Column(nullable = true)
    var deleteAt: LocalDateTime? = null

    @Column(nullable = true)
    var lastLoginFailAt: LocalDateTime? = null

    var locked: Boolean = false

    var loginFailCount: Int = 0

    @Column(nullable = true)
    var lockedUntil: LocalDateTime? = null

    // ========================
    // 보조 생성자
    // ========================

    /** SecurityUser용: id까지 세팅 */
    constructor(id: Int, username: String, nickname: String) : this(
        username,
        null,
        nickname,
        null,
        ""
    ) {
        this.id = id
    }

    constructor(id: Int, username: String, nickname: String, role: Role?) : this(
        username,
        null,
        nickname,
        role,
        ""
    ) {
        this.id = id
    }

    constructor(username: String, password: String?, nickname: String) : this(
        username,
        password,
        nickname,
        null,
        null
    )

    constructor(username: String, password: String?, nickname: String, profileImgUrl: String?) : this(
        username,
        password,
        nickname,
        null,
        profileImgUrl
    )

    /** 회원가입용: apiKey 자동 생성, status ACTIVE */
//    constructor(
//        username: String,
//        password: String?,
//        nickname: String,
//        role: Role,
//        profileImgUrl: String?,
//    ) : this(
//        username,
//        password,
//        nickname,
//        role,
//        profileImgUrl
//    ) {
//        this.status = MemberStatus.ACTIVE
//        this.loginFailCount = 0
//        this.apiKey = UUID.randomUUID().toString()
//        this.locked = false
//    }

    // ========================
    // 프로퍼티
    // ========================

    val isLocked: Boolean
        get() = locked && lockedUntil?.isAfter(LocalDateTime.now()) == true

    val isAdmin: Boolean
        get() = role == Role.ADMIN

    fun getName(): String =
        if (status == MemberStatus.WITHDRAWN) "탈퇴한 회원" else nickname

    val authorities: Collection<GrantedAuthority>
        get() = buildList {
            add(SimpleGrantedAuthority("ROLE_USER"))
            if (isAdmin) add(SimpleGrantedAuthority("ROLE_ADMIN"))
        }

    // ========================
    // 잠금 관련
    // ========================

    fun unlockIfExpired() {
        if (locked && lockedUntil?.isBefore(LocalDateTime.now()) == true) {
            locked = false
            lockedUntil = null
            loginFailCount = 0
        }
    }

    fun lock() { locked = true }
    fun resetFailCount() { loginFailCount = 0 }
    fun increaseFailCount() { loginFailCount++ }
    fun updateLastFailAt(now: LocalDateTime?) { lastLoginFailAt = now }
    fun lockUntil(after: LocalDateTime?) { lockedUntil = after }

    // ========================
    // 수정 관련
    // ========================

    fun modifyApiKey(apiKey: String) { this.apiKey = apiKey }
    fun modifyName(nickname: String) { this.nickname = nickname }
    fun modifyPassword(password: String?) { this.password = password }
    fun modify(nickname: String, profileImgUrl: String?) {
        this.nickname = nickname
        this.profileImgUrl = profileImgUrl
    }

    fun checkActorCanModify(actor: Member) {
        if (actor != this) throw ServiceException("403-1", "수정권한이 없습니다.")
    }

    // ========================
    // 상태 변경 관련
    // ========================

    fun changeStatus(target: MemberStatus) {
        if (!status.canTransitionTo(target)) throw ServiceException("400-4", "잘못된 상태 변경입니다.")
        status = target
    }

    fun suspend() { changeStatus(MemberStatus.SUSPENDED); suspendAt = LocalDateTime.now() }
    fun release() { changeStatus(MemberStatus.ACTIVE); suspendAt = null }
    fun banned() { changeStatus(MemberStatus.BANNED); deleteAt = LocalDateTime.now() }
    fun withdraw() { changeStatus(MemberStatus.WITHDRAWN); deleteAt = LocalDateTime.now() }
}
