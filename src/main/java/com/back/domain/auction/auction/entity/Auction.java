package com.back.domain.auction.auction.entity;

import com.back.domain.category.category.entity.Category;
import com.back.domain.member.member.entity.Member;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Auction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private Member seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "start_price", nullable = false)
    private Integer startPrice;

    @Column(name = "buy_now_price")
    private Integer buyNowPrice;

    @Column(name = "current_highest_bid")
    private Integer currentHighestBid;

    @Column(name = "bid_count", nullable = false)
    private Integer bidCount = 0;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private AuctionStatus status = AuctionStatus.OPEN;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Column(name = "winner_id")
    private Integer winnerId;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "cancelled_by")
    private Integer cancelledBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "canceller_role", length = 20)
    private CancellerRole cancellerRole;

    @OneToMany(mappedBy = "auction", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AuctionImage> auctionImages = new ArrayList<>();

    @Builder
    public Auction(Member seller, Category category, String name, String description,
                   Integer startPrice, Integer buyNowPrice, LocalDateTime startAt, LocalDateTime endAt) {
        this.seller = seller;
        this.category = category;
        this.name = name;
        this.description = description;
        this.startPrice = startPrice;
        this.buyNowPrice = buyNowPrice;
        this.startAt = startAt;
        this.endAt = endAt;
        this.bidCount = 0;
        this.status = AuctionStatus.OPEN;
    }

    public void addAuctionImage(AuctionImage auctionImage) {
        this.auctionImages.add(auctionImage);
        auctionImage.setAuction(this);
    }

    // 판매자 확인
    public boolean isSeller(Integer memberId) {
        return this.seller.getId() == memberId;
    }

    // 입찰 발생 여부 확인
    public boolean hasBids() {
        return this.bidCount > 0;
    }

    // 경매 수정 (입찰 전)
    public void updateBeforeBid(String name, String description, Integer startPrice,
                                 Integer buyNowPrice, LocalDateTime endAt) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        if (description != null) {
            this.description = description;
        }
        if (startPrice != null) {
            this.startPrice = startPrice;
        }
        if (buyNowPrice != null) {
            this.buyNowPrice = buyNowPrice;
        }
        if (endAt != null) {
            this.endAt = endAt;
        }
    }

    // 경매 수정 (입찰 후 - 제한적)
    public void updateAfterBid(String name, String description) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        if (description != null) {
            this.description = description;
        }
    }

    // 이미지 제거
    public void removeAuctionImage(AuctionImage auctionImage) {
        this.auctionImages.remove(auctionImage);
    }

    // 모든 이미지 제거
    public void clearAuctionImages() {
        this.auctionImages.clear();
    }

    // 입찰 정보 업데이트
    public void updateBid(Integer newPrice) {
        this.currentHighestBid = newPrice;
        this.bidCount++;
    }

    // 경매 즉시 종료 (즉시구매)
    public void closeAuction() {
        this.status = AuctionStatus.COMPLETED;
        this.closedAt = LocalDateTime.now();
    }

    // 낙찰 처리 (입찰이 있는 경우)
    public void completeWithWinner(Integer winnerId) {
        this.winnerId = winnerId;
        this.status = AuctionStatus.COMPLETED;
        this.closedAt = LocalDateTime.now();
    }

    // 경매 종료 (입찰이 없는 경우)
    public void closeWithoutBid() {
        this.status = AuctionStatus.CLOSED;
        this.closedAt = LocalDateTime.now();
    }

    // 거래 취소
    public void cancelTrade(Integer userId, CancellerRole role) {
        this.status = AuctionStatus.CANCELLED;
        this.cancelledBy = userId;
        this.cancellerRole = role;
        this.closedAt = LocalDateTime.now();
    }

    // 거래 취소 권한 및 역할 확인
    public CancellerRole determineCancellerRole(Integer memberId) {
        if (this.status != AuctionStatus.COMPLETED) {
            throw new IllegalStateException("낙찰 완료된 경매만 취소할 수 있습니다.");
        }

        if (this.seller.getId() == memberId) {
            return CancellerRole.SELLER;
        } else if (this.winnerId != null && this.winnerId == memberId) {
            return CancellerRole.BUYER;
        }

        throw new IllegalArgumentException("거래를 취소할 권한이 없습니다.");
    }

    // 거래 취소 권한 확인 (판매자 또는 낙찰자)
    public boolean canCancelTrade(Integer memberId) {
        if (this.status != AuctionStatus.COMPLETED) {
            return false;
        }
        return this.seller.getId() == memberId || (this.winnerId != null && this.winnerId == memberId);
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.endAt);
    }

    // 경매 진행 중 여부 확인
    public boolean isActive() {
        return this.status == AuctionStatus.OPEN && !isExpired();
    }

    // 낙찰 완료 여부 확인
    public boolean isCompleted() {
        return this.status == AuctionStatus.COMPLETED;
    }

    // 경매 종료 여부 확인 (CLOSED, COMPLETED, CANCELLED)
    public boolean isClosed() {
        return this.status == AuctionStatus.CLOSED ||
               this.status == AuctionStatus.COMPLETED ||
               this.status == AuctionStatus.CANCELLED;
    }
}

