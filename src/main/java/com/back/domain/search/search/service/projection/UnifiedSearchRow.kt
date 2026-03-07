package com.back.domain.search.search.service.projection

import java.time.LocalDateTime

interface UnifiedSearchRow {
    val id: Int
    val type: String
    val title: String
    val price: Int
    val status: String
    val statusDisplayName: String?
    val categoryId: Int
    val thumbnailUrl: String?
    val createDate: LocalDateTime

    val score: Double?   // ✅ 추가 (MySQL MATCH는 보통 double)
    val typeRank: Int?   // ✅ 추가 (id 충돌 방지 tie-breaker)
}