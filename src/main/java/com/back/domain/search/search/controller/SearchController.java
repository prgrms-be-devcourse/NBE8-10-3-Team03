package com.back.domain.search.search.controller;

import com.back.domain.search.search.dto.UnifiedSearchResponse;
import com.back.domain.search.search.service.SearchService;
import com.back.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {
    private final SearchService searchService;

    @GetMapping
    public RsData<Page<UnifiedSearchResponse>> search(
        @RequestParam String keyword,
        @PageableDefault(
            size = 10, 
            sort = "createDate", 
            direction = Sort.Direction.DESC
        ) Pageable pageable
    ) {
        Page<UnifiedSearchResponse> results = searchService.searchUnified(keyword, pageable);
        
        return new RsData<>(
            "200-1", 
            "검색이 완료되었습니다.", 
            results
        );
    }
}