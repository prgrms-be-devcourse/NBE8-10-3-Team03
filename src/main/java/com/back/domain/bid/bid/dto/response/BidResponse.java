package com.back.domain.bid.bid.dto.response;

import com.back.domain.bid.bid.entity.Bid;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class BidResponse {
    private final Integer bidId;
    private final Integer auctionId;
    private final Integer bidderId;
    private final String bidderNickname;
    private final Integer price;
    private final Integer currentHighestBid;
    private final Integer bidCount;
    private final LocalDateTime createdAt;
    private final boolean isBuyNow; // 즉시구매 여부

    public BidResponse(Bid bid, Integer currentHighestBid, Integer bidCount, boolean isBuyNow) {
        this.bidId = bid.getId();
        this.auctionId = bid.getAuction().getId();
        this.bidderId = bid.getBidder().getId();
        this.bidderNickname = bid.getBidder().getNickname();
        this.price = bid.getPrice();
        this.currentHighestBid = currentHighestBid;
        this.bidCount = bidCount;
        this.createdAt = bid.getCreatedAt();
        this.isBuyNow = isBuyNow;
    }
}

