package com.back.domain.member.member.entity;

import com.back.domain.member.member.enums.Role;
import com.back.domain.member.member.enums.MemberStatus;
import com.back.domain.member.reputation.entity.Reputation;
import com.back.domain.member.reputation.entity.ReputationEvent;
import com.back.domain.member.review.entity.Review;
import com.back.global.exception.ServiceException;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "users")
@NoArgsConstructor
public class Member extends BaseEntity {
    @Column(unique = true)
    private String username;

    private String password;

    @Column(unique = true)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberStatus status;

    @Column(unique = true)
    private String apiKey;

    private String profileImgUrl;

    @OneToOne(mappedBy = "member", fetch = FetchType.LAZY)
    private Reputation reputation;

    @OneToMany(mappedBy = "target", fetch = FetchType.LAZY)
    private List<ReputationEvent> targetEvents;

    @OneToMany(mappedBy = "reporter", fetch = FetchType.LAZY)
    private List<ReputationEvent> reporterEvents;

    @OneToMany(mappedBy = "member", fetch = FetchType.LAZY)
    private List<Review> reviews;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = true)
    private LocalDateTime suspendAt;  // 정지 시각

    @Column(nullable = true)
    private LocalDateTime deleteAt;  // 영구 정지 시각


    private int loginFailCount;
    @Column(nullable = true)
    private LocalDateTime lastLoginFailAt;
    private boolean locked;
    @Column(nullable = true)
    private LocalDateTime lockedUntil;

    // 계정 잠김 관련
    public boolean isLocked() {
        return locked && lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now());
    }

    public void unlockIfExpired() {
        if (locked && lockedUntil.isBefore(LocalDateTime.now())) {
            this.locked = false;
            this.lockedUntil = null;
            this.loginFailCount = 0;
        }
    }

    public void lock() {
        this.locked = true;
    }

    public void resetFailCount() {
        this.loginFailCount = 0;
    }

    public void increaseFailCount() {
        this.loginFailCount++;
    }

    public void updateLastFailAt(LocalDateTime now) {
        this.lastLoginFailAt = now;
    }

    public void lockUntil(LocalDateTime after) {
        this.lockedUntil = after;
    }


    // 생성자
    public Member(int id, String username, String name, Role role) {
        setId(id);
        this.username = username;
        this.nickname = name;
        this.role = role;
    }

    public Member(String username, String password, String nickname) {
        this(username, password, nickname, null);
    }

    public Member(String username, String password, String nickname, String profileImgUrl) {
        this.username = username;
        this.password = password;
        this.nickname = nickname;
        this.status = MemberStatus.ACTIVE;
        this.apiKey = UUID.randomUUID().toString();
        this.profileImgUrl = profileImgUrl;
        this.loginFailCount = 0;
        this.locked = false;
        setRole(Role.USER);
    }


    public MemberStatus getActive() {
        return this.status;
    }

    public String getNickname() {
        return this.status == MemberStatus.WITHDRAWN ? "탈퇴한 회원" : this.nickname;
    }


    public void checkActorCanModify(Member actor) {
        if (!actor.equals(this))
            throw new ServiceException("403-1", "수정권한이 없습니다.".formatted(getId()));
    }


    // 수정 관련
    public void modifyApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public void modifyName(String nickname) {
        this.nickname = nickname;
    }

    public void modifyPassword(String password) {
        this.password = password;
    }

    public void modify(String nickname, String profileImgUrl) {
        this.nickname = nickname;
        this.profileImgUrl = profileImgUrl;
    }


    // 계정 활성화 관련
    // 상태 변경 가능한지
    public void changeStatus(MemberStatus target) {
        if (!this.status.canTransitionTo(target)) {
            throw new ServiceException("400-4", "잘못된 상태 변경입니다.");
        }

        this.status = target;
    }

    // 계정 정지
    public void suspend() {
        changeStatus(MemberStatus.SUSPENDED);
        this.suspendAt = LocalDateTime.now();
    }

    // 계정 재활성화
    public void release() {
        changeStatus(MemberStatus.ACTIVE);
        this.suspendAt = null;
    }

    // 계정 영구 정지
    public void banned() {
        changeStatus(MemberStatus.BANNED);
        this.deleteAt = LocalDateTime.now();
    }

    // 계정 탈퇴
    public void withdraw() {
        changeStatus(MemberStatus.WITHDRAWN);
        this.deleteAt = LocalDateTime.now();
    }


    // 계정 인가 관련
    public boolean isAdmin() {
        if (this.role == Role.ADMIN) return true;

        return false;
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        return getAuthoritiesAsStringList()
                .stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
    }


    private List<String> getAuthoritiesAsStringList() {
        List<String> authorities = new ArrayList<>();

        authorities.add("ROLE_USER");

        if (isAdmin()) {
            authorities.add("ROLE_ADMIN");
        }

        return authorities;
    }
}