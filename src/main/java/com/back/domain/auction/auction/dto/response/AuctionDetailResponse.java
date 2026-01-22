package com.back.domain.auction.auction.dto.response;

import com.back.domain.auction.auction.entity.Auction;
import com.back.domain.auction.auction.entity.AuctionStatus;
import com.back.domain.auction.auction.entity.CancellerRole;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class AuctionDetailResponse {
    private final Integer auctionId;
    private final String name;
    private final String description;
    private final Integer startPrice;
    private final Integer currentHighestBid;
    private final Integer buyNowPrice;
    private final Integer bidCount;
    private final AuctionStatus status;
    private final LocalDateTime startAt;
    private final LocalDateTime endAt;
    private final List<String> imageUrls;
    private final SellerDto seller;
    private final String categoryName;

    // 낙찰 정보
    private final Integer winnerId;
    private final LocalDateTime closedAt;

    // 취소 정보
    private final Integer cancelledBy;
    private final CancellerRole cancellerRole;
    private final String cancellerRoleDescription;

    public AuctionDetailResponse(Auction auction) {
        this.auctionId = auction.getId();
        this.name = auction.getName();
        this.description = auction.getDescription();
        this.startPrice = auction.getStartPrice();
        this.currentHighestBid = auction.getCurrentHighestBid();
        this.buyNowPrice = auction.getBuyNowPrice();
        this.bidCount = auction.getBidCount();
        this.status = auction.getStatus();
        this.startAt = auction.getStartAt();
        this.endAt = auction.getEndAt();
        this.categoryName = auction.getCategory().getName();

        // 낙찰 정보
        this.winnerId = auction.getWinnerId();
        this.closedAt = auction.getClosedAt();

        // 취소 정보
        this.cancelledBy = auction.getCancelledBy();
        this.cancellerRole = auction.getCancellerRole();
        this.cancellerRoleDescription = auction.getCancellerRole() != null ?
                auction.getCancellerRole().getDescription() : null;

        // 이미지 URL 목록 추출
        this.imageUrls = auction.getAuctionImages().stream()
                .map(auctionImage -> auctionImage.getImage().getUrl())
                .collect(Collectors.toList());

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

