package com.back.domain.auction.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuctionIdResponse {
    private Integer auctionId;
    private String message;
}

