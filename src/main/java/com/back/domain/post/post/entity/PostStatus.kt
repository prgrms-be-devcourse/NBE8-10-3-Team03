package com.back.domain.post.post.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PostStatus {
    SALE("판매 중"),
    RESERVED("예약 중"),
    SOLD("판매 완료");

    private final String displayName;
}