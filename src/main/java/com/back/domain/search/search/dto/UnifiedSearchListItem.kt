package com.back.domain.search.search.dto

data class UnifiedSearchListItem(
    val id: Int,
    val type: String,   // "AUCTION" | "POST"
    val title: String,  // 상품명
    val price: Int,
    val status: String  // 경매/거래 상태(문자열)
)