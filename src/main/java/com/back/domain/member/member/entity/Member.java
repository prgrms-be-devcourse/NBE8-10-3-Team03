package com.back.domain.member.member.entity;

import com.back.domain.member.member.enums.Role;
import com.back.global.exception.ServiceException;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Table(name = "users")
@NoArgsConstructor
public class Member extends BaseEntity {
    @Column(unique = true)
    private String username;

    private String password;

    @Column(unique = true)
    private String nickname;

    private Boolean isActive;

    @Column(unique = true)
    private String apiKey;

    private String profileImgUrl;

    @OneToOne(mappedBy = "member", fetch = FetchType.LAZY)
    private Reputation reputation;

    @OneToMany(mappedBy = "member", fetch = FetchType.LAZY)
    private List<ReputationEvent> reputationEvents;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

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
        this.isActive = true;
        this.apiKey = UUID.randomUUID().toString();
        this.profileImgUrl = profileImgUrl;
        setRole(Role.USER);
    }

    public String getName() {
        return nickname;
    }

    public void setName(String name) {
        this.nickname = name;
    }

    public void modifyApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

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

    public void checkActorCanModify(Member actor) {
        if (!actor.equals(this))
            throw new ServiceException("403-1", "수정권한이 없습니다.".formatted(getId()));
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
}