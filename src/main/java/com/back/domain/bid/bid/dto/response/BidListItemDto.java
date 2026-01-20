package com.back.domain.bid.bid.dto.response;

import com.back.domain.bid.bid.entity.Bid;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class BidListItemDto {
    private final Integer bidId;
    private final Integer bidderId;
    private final String bidderNickname;
    private final Integer price;
    private final LocalDateTime createdAt;

    public BidListItemDto(Bid bid) {
        this.bidId = bid.getId();
        this.bidderId = bid.getBidder().getId();
        this.bidderNickname = bid.getBidder().getNickname();
        this.price = bid.getPrice();
        this.createdAt = bid.getCreatedAt();
    }
}

