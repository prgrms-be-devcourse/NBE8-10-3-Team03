package com.back.domain.post.post.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class PostPageResponse {
    private List<PostListResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private String currentStatusFilter;
}