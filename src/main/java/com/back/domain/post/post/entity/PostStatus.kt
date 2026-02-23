package com.back.domain.post.post.entity

enum class PostStatus(val displayName: String) {
    SALE("판매 중"),
    RESERVED("예약 중"),
    SOLD("판매 완료")
}