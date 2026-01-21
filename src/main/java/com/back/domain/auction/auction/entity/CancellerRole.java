package com.back.domain.auction.auction.entity;

public enum CancellerRole {
    SELLER("판매자"),
    BUYER("구매자");

    private final String description;

    CancellerRole(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

