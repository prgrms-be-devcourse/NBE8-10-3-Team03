package com.back.domain.bid.bid.dto.response

import com.back.domain.bid.bid.entity.Bid
import java.time.LocalDateTime

data class BidResponse(
    val bidId: Int,
    val auctionId: Int,
    val bidderId: Int,
    val bidderNickname: String,
    val price: Int,
    val currentHighestBid: Int,
    val bidCount: Int,
    val createdAt: LocalDateTime,
    val isBuyNow: Boolean
) {
    companion object {
        fun from(
            bid: Bid,
            currentHighestBid: Int,
            bidCount: Int,
            isBuyNow: Boolean
        ): BidResponse = BidResponse(
            bidId = bid.id,
            auctionId = bid.auction.id,
            bidderId = bid.bidder.id,
            bidderNickname = bid.bidder.nickname,
            price = bid.price,
            currentHighestBid = currentHighestBid,
            bidCount = bidCount,
            createdAt = bid.createdAt,
            isBuyNow = isBuyNow
        )
    }
}
