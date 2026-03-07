package com.back.domain.search.search.dto

import java.time.LocalDateTime

data class UnifiedSearchResponse(
    val id: Int,
    val type: String,         // "POST" 또는 "AUCTION"
    val title: String,        // 공통 제목 (Post: title / Auction: name)
    val price: Int,           // 공통 가격
    val status: String,       // 공통 상태
    val statusDisplayName: String? = null,
    val categoryId: Int, // 공통 카테고리명
    val thumbnailUrl: String?,// 공통 대표 이미지
    val createDate: LocalDateTime,
    val viewCount: Long? = null,
    val sellerId: Int? = null,
    val sellerNickname: String? = null,
    val sellerBadge: String? = null
)