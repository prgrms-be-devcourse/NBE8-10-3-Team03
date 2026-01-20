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
@Table(name = "auctions")
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

    @Column(nullable = false)
    private Boolean completed = false;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

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
        this.completed = false;
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
}