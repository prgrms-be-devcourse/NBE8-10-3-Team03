package com.back.domain.auction.auction.dto.response

import com.back.domain.auction.auction.entity.Auction
import com.back.domain.auction.auction.entity.AuctionStatus
import java.time.LocalDateTime

class AuctionListItemDto(
    auction: Auction,
    val thumbnailUrl: String?
) {
    val auctionId: Int = auction.id
    val name: String = auction.name
    val startPrice: Int? = auction.startPrice
    val currentHighestBid: Int? = auction.currentHighestBid
    val buyNowPrice: Int? = auction.buyNowPrice
    val status: AuctionStatus = auction.status
    val endAt: LocalDateTime = auction.endAt
    val bidCount: Int = auction.bidCount
    val seller: SellerDto
    val categoryName: String = auction.category.name

    init {
        val reputationScore = auction.seller.reputation?.score
        seller = SellerDto(
            auction.seller.id,
            auction.seller.nickname,
            reputationScore
        )
    }
}
