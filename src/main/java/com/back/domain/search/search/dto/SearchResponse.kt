package com.back.domain.search.search.dto

data class SearchResponse(
    val content: List<UnifiedSearchResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)