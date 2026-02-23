package com.back.domain.auction.auction.entity

import com.back.domain.image.image.entity.Image
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "auction_images")
@IdClass(AuctionImageId::class)
class AuctionImage() {
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id")
    lateinit var auction: Auction

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id")
    lateinit var image: Image

    constructor(auction: Auction, image: Image) : this() {
        this.auction = auction
        this.image = image
    }

    fun setAuction(auction: Auction) {
        this.auction = auction
    }
}
