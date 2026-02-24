package com.back.domain.search.search.controller

import com.back.domain.search.search.dto.SearchResponse
import com.back.domain.search.search.service.SearchService
import com.back.global.rsData.RsData
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
        @PageableDefault(
            size = 10,
            sort = ["createDate"],
            direction = Sort.Direction.DESC
        ) pageable: Pageable
    ): RsData<SearchResponse> {
        val results = searchService.searchUnified(keyword, pageable)

        val response = SearchResponse(
            content = results.content,
            page = results.number,
            size = results.size,
            totalElements = results.totalElements,
            totalPages = results.totalPages
        )

        return RsData(
            "200-1",
            "검색이 완료되었습니다.",
            response
        )
    }
}