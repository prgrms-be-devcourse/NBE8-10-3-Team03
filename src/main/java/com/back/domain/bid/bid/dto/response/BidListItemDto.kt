package com.back.domain.bid.bid.dto.response

import com.back.domain.bid.bid.entity.Bid
import java.time.LocalDateTime

data class BidListItemDto(
    val bidId: Int,
    val bidderId: Int,
    val bidderNickname: String,
    val price: Int,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(bid: Bid): BidListItemDto = BidListItemDto(
            bidId = bid.id,
            bidderId = bid.bidder.id,
            bidderNickname = bid.bidder.nickname,
            price = bid.price,
            createdAt = bid.createdAt
        )
    }
}
