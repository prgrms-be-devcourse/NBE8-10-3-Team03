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
import lombok.Getter
import lombok.NoArgsConstructor
import lombok.Setter
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.time.LocalDateTime
import java.util.*

@Entity
@Getter
@Setter
@Table(name = "users")
@NoArgsConstructor
class Member : BaseEntity {
    @Column(unique = true)
    var username: String?
        private set

    var password: String? = null
        private set

    @Column(unique = true)
    private var nickname: String?

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private var status: MemberStatus? = null

    @Column(unique = true)
    var apiKey: String? = null
        private set

    var profileImgUrl: String? = null
        private set

    @OneToOne(mappedBy = "member", fetch = FetchType.LAZY)
    var reputation: Reputation? = null
        private set

    @OneToMany(mappedBy = "target", fetch = FetchType.LAZY)
    private var targetEvents: MutableList<ReputationEvent?>? = null

    @OneToMany(mappedBy = "target", fetch = FetchType.LAZY)
    var targetReports: MutableList<Report?>? = null
        private set

    @OneToMany(mappedBy = "reporter", fetch = FetchType.LAZY)
    private var reporterReports: MutableList<Report?>? = null

    @OneToMany(mappedBy = "member", fetch = FetchType.LAZY)
    var reviews: MutableList<Review?>? = null
        private set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var role: Role?
        private set

    @Column(nullable = true)
    private var suspendAt: LocalDateTime? = null // 정지 시각

    @Column(nullable = true)
    private var deleteAt: LocalDateTime? = null // 영구 정지 시각


    var loginFailCount: Int = 0
        private set

    @Column(nullable = true)
    private var lastLoginFailAt: LocalDateTime? = null
    private var locked = false

    @Column(nullable = true)
    var lockedUntil: LocalDateTime? = null
        private set

    // 계정 잠김 관련
    fun isLocked(): Boolean {
        return locked && lockedUntil != null && lockedUntil!!.isAfter(LocalDateTime.now())
    }

    fun unlockIfExpired() {
        if (locked && lockedUntil!!.isBefore(LocalDateTime.now())) {
            this.locked = false
            this.lockedUntil = null
            this.loginFailCount = 0
        }
    }

    fun lock() {
        this.locked = true
    }

    fun resetFailCount() {
        this.loginFailCount = 0
    }

    fun increaseFailCount() {
        this.loginFailCount++
    }

    fun updateLastFailAt(now: LocalDateTime?) {
        this.lastLoginFailAt = now
    }

    fun lockUntil(after: LocalDateTime?) {
        this.lockedUntil = after
    }


    // 생성자
    constructor(id: Int, username: String?, name: String?, role: Role?) {
        setId(id)
        this.username = username
        this.nickname = name
        this.role = role
    }

    @JvmOverloads
    constructor(username: String?, password: String?, nickname: String?, profileImgUrl: String? = null) : this(
        username,
        password,
        nickname,
        null,
        profileImgUrl
    )

    constructor(username: String?, password: String?, nickname: String?, role: Role?, profileImgUrl: String?) {
        this.username = username
        this.password = password
        this.nickname = nickname
        this.status = MemberStatus.ACTIVE
        this.apiKey = UUID.randomUUID().toString()
        this.role = role
        this.profileImgUrl = profileImgUrl
        this.loginFailCount = 0
        this.locked = false
    }


    val active: MemberStatus
        get() = this.status!!

    fun getNickname(): String? {
        return if (this.status == MemberStatus.WITHDRAWN) "탈퇴한 회원" else this.nickname
    }


    fun checkActorCanModify(actor: Member) {
        if (actor != this) throw ServiceException("403-1", "수정권한이 없습니다.".formatted(getId()))
    }


    // 수정 관련
    fun modifyApiKey(apiKey: String?) {
        this.apiKey = apiKey
    }

    fun modifyName(nickname: String?) {
        this.nickname = nickname
    }

    fun modifyPassword(password: String?) {
        this.password = password
    }

    fun modify(nickname: String?, profileImgUrl: String?) {
        this.nickname = nickname
        this.profileImgUrl = profileImgUrl
    }


    // 계정 활성화 관련
    // 상태 변경 가능한지
    fun changeStatus(target: MemberStatus) {
        if (!this.status!!.canTransitionTo(target)) {
            throw ServiceException("400-4", "잘못된 상태 변경입니다.")
        }

        this.status = target
    }

    // 계정 정지
    fun suspend() {
        changeStatus(MemberStatus.SUSPENDED)
        this.suspendAt = LocalDateTime.now()
    }

    // 계정 재활성화
    fun release() {
        changeStatus(MemberStatus.ACTIVE)
        this.suspendAt = null
    }

    // 계정 영구 정지
    fun banned() {
        changeStatus(MemberStatus.BANNED)
        this.deleteAt = LocalDateTime.now()
    }

    // 계정 탈퇴
    fun withdraw() {
        changeStatus(MemberStatus.WITHDRAWN)
        this.deleteAt = LocalDateTime.now()
    }


    val isAdmin: Boolean
        // 계정 인가 관련
        get() {
            if (this.role == Role.ADMIN) return true

            return false
        }

    val authorities: MutableCollection<out GrantedAuthority?>
        get() = this.authoritiesAsStringList
            .stream()
            .map<SimpleGrantedAuthority?> { authority: String? -> SimpleGrantedAuthority(authority!!) }
            .toList()


    private val authoritiesAsStringList: MutableList<String?>
        get() {
            val authorities: MutableList<String?> = ArrayList<String?>()

            authorities.add("ROLE_USER")

            if (this.isAdmin) {
                authorities.add("ROLE_ADMIN")
            }

            return authorities
        }


    fun getStatus(): MemberStatus {
        return status!!
    }
}