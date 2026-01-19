package com.back.domain.auction.auction.entity;

import com.back.domain.image.image.entity.Image;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "auction_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@IdClass(AuctionImageId.class)
public class AuctionImage {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id")
    private Auction auction;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id")
    private Image image;

    public AuctionImage(Auction auction, Image image) {
        this.auction = auction;
        this.image = image;
    }

    public void setAuction(Auction auction) {
        this.auction = auction;
    }
}

