package com.back.domain.search.search.dto

import org.springframework.data.domain.Slice

data class SearchResponse(
    val content: List<UnifiedSearchResponse>,
    val page: Int,                 // 커서 방식이면 의미 없으니 0 고정으로 내려줌
    val size: Int,
    val hasNext: Boolean,
    val nextCursor: String? // 커서 방식일 때만 채움
) {
//    companion object {
//        // 일반 Slice(Pageable 기반) 응답
//        fun of(slice: Slice<UnifiedSearchResponse>) = SearchResponse(
//            content = slice.content,
//            page = slice.number,
//            size = slice.size,
//            hasNext = slice.hasNext(),
//            nextCursor = null
//        )
//
//        // 커서 기반 응답
//        fun ofCursor(
//            content: List<UnifiedSearchResponse>,
//            size: Int,
//            hasNext: Boolean,
//            nextCursor: String?
//        ) = SearchResponse(
//            content = content,
//            page = 0, // 커서 방식은 page 개념이 없으니 0 고정
//            size = size,
//            hasNext = hasNext,
//            nextCursor = if (hasNext) nextCursor else null
//        )
//    }
}