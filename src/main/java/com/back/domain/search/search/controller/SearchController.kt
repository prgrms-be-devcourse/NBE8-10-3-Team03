package com.back.domain.search.search.controller

import com.back.domain.search.search.dto.SearchResponse
import com.back.domain.search.search.service.SearchService
import com.back.global.rsData.RsData
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/search")
class SearchController(
    private val searchService: SearchService
) {

    @GetMapping
    fun search(
        @RequestParam keyword: String,
        @RequestParam(defaultValue = "relevance") sort: String,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) cursor: String?
    ): RsData<SearchResponse> {
        val safeKeyword = keyword.trim()
        val safeSize = size.coerceIn(1, 50)
        val safeSort = when (sort.lowercase()) {
            "relevance", "newest", "oldest" -> sort.lowercase()
            else -> "relevance"
        }

        val result = searchService.searchUnifiedCursor(
            keyword = safeKeyword,
            sort = safeSort,
            size = safeSize,
            cursor = cursor
        )

        return RsData(
            "200-1",
            "검색이 완료되었습니다.",
            result
        )
    }

//    @GetMapping
//    fun search(
//        @RequestParam keyword: String,
//        @RequestParam(defaultValue = "relevance") sort: String,   // relevance|newest|oldest
//        @RequestParam(defaultValue = "20") size: Int,
//        @RequestParam(required = false) cursor: String?           // 커서(없으면 첫 페이지)
//    ): RsData<SearchResponse> {
//
//        val safeSize = size.coerceIn(1, 50)
//        val safeSort = when (sort.lowercase()) {
//            "relevance", "newest", "oldest" -> sort.lowercase()
//            else -> "relevance"
//        }
//
//        // 커서 기반 검색 서비스 (Pageable/offset 없이 진행)
//        val result: SearchResponse = searchService.searchUnifiedCursor(
//            keyword = keyword,
//            sort = safeSort,
//            size = safeSize,
//            cursor = cursor
//        )
//
//        return RsData(
//            "200-1",
//            "검색이 완료되었습니다.",
//            result
//        )
//    }
}