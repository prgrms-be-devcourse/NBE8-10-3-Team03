package com.back.global.security;

import com.back.domain.member.member.enums.Role;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

public class SecurityUser extends User implements OAuth2User {
    @Getter
    private int id;
    @Getter
    private String name;
    @Getter
    private Role role;

    public SecurityUser(
            int id,
            String username,
            String password,
            String name,
            Role role,
            Collection<? extends GrantedAuthority> authorities
    ) {
        super(username, password != null ? password : "", authorities);
        this.id = id;
        this.name = name;
        this.role = role;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return Map.of();
    }
}