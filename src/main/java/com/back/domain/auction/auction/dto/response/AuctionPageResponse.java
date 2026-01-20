package com.back.domain.auction.auction.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@AllArgsConstructor
public class AuctionPageResponse {
    private List<AuctionListItemDto> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    public static AuctionPageResponse from(Page<AuctionListItemDto> page) {
        return new AuctionPageResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}

