package com.back.domain.bid.bid.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@AllArgsConstructor
public class BidPageResponse {
    private List<BidListItemDto> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    public static BidPageResponse from(Page<BidListItemDto> page) {
        return new BidPageResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}


