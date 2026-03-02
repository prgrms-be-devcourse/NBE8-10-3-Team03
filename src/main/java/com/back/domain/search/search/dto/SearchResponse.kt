package com.back.domain.search.search.dto

import org.springframework.data.domain.Slice

data class SearchResponse(
    val content: List<UnifiedSearchListItem>,
    val page: Int,                 // 커서 방식이면 의미 없으니 0 고정
    val size: Int,
    val hasNext: Boolean,
    val nextCursor: String? = null // 커서 방식일 때만 채움
) {
    companion object {
        // ✅ 일반 Slice(ListItem) 기반 응답 (offset/page 방식에 쓰고 싶을 때)
        fun of(slice: Slice<UnifiedSearchListItem>) = SearchResponse(
            content = slice.content,
            page = slice.number,
            size = slice.size,
            hasNext = slice.hasNext(),
            nextCursor = null
        )

        // ✅ 커서 기반 응답
        fun ofCursor(
            content: List<UnifiedSearchListItem>,
            size: Int,
            hasNext: Boolean,
            nextCursor: String?
        ) = SearchResponse(
            content = content,
            page = 0,
            size = size,
            hasNext = hasNext,
            nextCursor = if (hasNext) nextCursor else null
        )
    }
}