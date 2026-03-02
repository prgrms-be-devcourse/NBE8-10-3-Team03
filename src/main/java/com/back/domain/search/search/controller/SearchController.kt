package com.back.domain.search.search.controller

import com.back.domain.search.search.dto.SearchListResponse
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
    ): RsData<SearchListResponse> {

        val safeSort = when (sort.lowercase()) {
            "relevance", "newest", "oldest" -> sort.lowercase()
            else -> "relevance"
        }

        val result = searchService.searchUnifiedListCursor(
            keyword = keyword,
            sort = safeSort,
            size = size,
            cursor = cursor
        )

        return RsData("200-1", "검색이 완료되었습니다.", result)
    }
}