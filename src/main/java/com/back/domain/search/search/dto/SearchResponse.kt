package com.back.domain.search.search.dto

import org.springframework.data.domain.Page

data class SearchResponse(
    val content: List<UnifiedSearchResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
) {
    companion object {
        fun of(page: Page<UnifiedSearchResponse>) = SearchResponse(
            content = page.content,
            page = page.number,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages
        )
    }
}