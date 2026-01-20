package com.back.domain.bid.bid.entity;

import com.back.domain.auction.auction.entity.Auction;
import com.back.domain.member.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "bids")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Bid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bidder_id", nullable = false)
    private Member bidder;

    @Column(nullable = false)
    private Integer price;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Bid(Auction auction, Member bidder, Integer price) {
        this.auction = auction;
        this.bidder = bidder;
        this.price = price;
        this.createdAt = LocalDateTime.now();
    }
}