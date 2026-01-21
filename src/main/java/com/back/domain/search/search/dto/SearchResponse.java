package com.back.domain.search.search.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class SearchResponse {
    private List<UnifiedSearchResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    public static SearchResponse of(List<UnifiedSearchResponse> content, int page, int size, long totalElements, int totalPages) {
        return new SearchResponse(content, page, size, totalElements, totalPages);
    }
}

