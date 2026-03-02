package com.back.domain.auction.auction.dto.response

import com.back.domain.auction.auction.entity.Auction
import com.back.domain.auction.auction.entity.AuctionStatus
import java.time.LocalDateTime

data class AuctionListItemDto(
    val auctionId: Int,
    val name: String,
    val startPrice: Int?,
    val currentHighestBid: Int?,
    val buyNowPrice: Int?,
    val status: AuctionStatus,
    val endAt: LocalDateTime,
    val bidCount: Int,
    val seller: SellerDto,
    val categoryName: String,
    val thumbnailUrl: String?
) {
    constructor(auction: Auction) : this(
        auctionId = auction.id,
        name = auction.name,
        startPrice = auction.startPrice,
        currentHighestBid = auction.currentHighestBid,
        buyNowPrice = auction.buyNowPrice,
        status = auction.status,
        endAt = auction.endAt,
        bidCount = auction.bidCount,
        seller = SellerDto(
            auction.seller.id,
            auction.seller.nickname,
            auction.seller.reputation?.score
        ),
        categoryName = auction.category.name,
        thumbnailUrl = auction.thumbnailUrl
    )
}
