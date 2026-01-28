package com.back.domain.auction.auction.dto.response;

import com.back.domain.auction.auction.entity.Auction;
import com.back.domain.auction.auction.entity.AuctionStatus;
import com.back.domain.auction.auction.entity.CancellerRole;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor  // Jackson 역직렬화를 위한 기본 생성자
public class AuctionDetailResponse {
    private Integer auctionId;
    private String name;
    private String description;
    private Integer startPrice;
    private Integer currentHighestBid;
    private Integer buyNowPrice;
    private Integer bidCount;
    private AuctionStatus status;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private List<String> imageUrls;
    private SellerDto seller;
    private String categoryName;

    // 낙찰 정보
    private Integer winnerId;
    private LocalDateTime closedAt;

    // 취소 정보
    private Integer cancelledBy;
    private CancellerRole cancellerRole;
    private String cancellerRoleDescription;

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

