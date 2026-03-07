package com.back.domain.search.search.dto

import java.time.LocalDateTime

data class UnifiedSearchResponse(
    val id: Int,
    val type: String,
    val title: String,
    val price: Int?,
    val status: String?,
    val statusDisplayName: String?,
    val categoryId: Int?,
    val thumbnailUrl: String?,
    val createDate: LocalDateTime,
    val score: Double?
)