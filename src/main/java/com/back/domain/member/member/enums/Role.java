package com.back.domain.member.member.enums;

public enum Role {
    ADMIN,
    USER;

    public static Role from(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Role is null");
        }

        try {
            return Role.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + value);
        }
    }
}
