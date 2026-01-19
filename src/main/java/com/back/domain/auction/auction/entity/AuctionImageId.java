package com.back.domain.auction.auction.entity;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@NoArgsConstructor
@EqualsAndHashCode
public class AuctionImageId implements Serializable {
    private Integer auction;
    private Integer image;

    public AuctionImageId(Integer auction, Integer image) {
        this.auction = auction;
        this.image = image;
    }
}

