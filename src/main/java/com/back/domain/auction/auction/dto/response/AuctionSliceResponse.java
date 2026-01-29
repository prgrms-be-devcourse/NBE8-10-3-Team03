package com.back.domain.auction.auction.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Slice;

import java.util.List;

@Getter
@AllArgsConstructor
public class AuctionSliceResponse {
    private List<AuctionListItemDto> content;
    private int page;
    private int size;
    private boolean hasNext;

    public static AuctionSliceResponse from(Slice<AuctionListItemDto> page) {
        return new AuctionSliceResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.hasNext()
        );
    }
}

