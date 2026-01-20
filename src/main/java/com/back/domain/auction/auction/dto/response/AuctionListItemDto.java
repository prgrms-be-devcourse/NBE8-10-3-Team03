package com.back.domain.auction.auction.dto.response;

import com.back.domain.auction.auction.entity.Auction;
import com.back.domain.auction.auction.entity.AuctionStatus;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class AuctionListItemDto {
    private final Integer auctionId;
    private final String name;
    private final String thumbnailUrl;
    private final Integer startPrice;
    private final Integer currentHighestBid;
    private final Integer buyNowPrice;
    private final AuctionStatus status;
    private final LocalDateTime endAt;
    private final Integer bidCount;
    private final SellerDto seller;
    private final String categoryName;

    public AuctionListItemDto(Auction auction, String thumbnailUrl) {
        this.auctionId = auction.getId();
        this.name = auction.getName();
        this.thumbnailUrl = thumbnailUrl;
        this.startPrice = auction.getStartPrice();
        this.currentHighestBid = auction.getCurrentHighestBid();
        this.buyNowPrice = auction.getBuyNowPrice();
        this.status = auction.getStatus();
        this.endAt = auction.getEndAt();
        this.bidCount = auction.getBidCount();
        this.categoryName = auction.getCategory().getName();

        // 판매자 정보 및 신용도
        Double reputationScore = null;
        if (auction.getSeller().getReputation() != null) {
            reputationScore = auction.getSeller().getReputation().getScore();
        }

        this.seller = new SellerDto(
                auction.getSeller().getId(),
                auction.getSeller().getNickname(),
                reputationScore
        );
    }
}

