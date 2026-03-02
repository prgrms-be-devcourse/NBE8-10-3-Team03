package com.back.domain.search.search.dto

data class SearchListResponse(
    val content: List<UnifiedSearchListItem>,
    val size: Int,
    val hasNext: Boolean,
    val nextCursor: String? = null
)