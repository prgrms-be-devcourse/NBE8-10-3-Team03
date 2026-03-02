package com.back.domain.auction.auction.dto.response

import com.back.domain.auction.auction.entity.AuctionStatus
import java.time.LocalDateTime

data class AuctionListProjection(
    val auctionId: Int,
    val name: String,
    val startPrice: Int?,
    val currentHighestBid: Int?,
    val buyNowPrice: Int?,
    val status: AuctionStatus,
    val endAt: LocalDateTime,
    val bidCount: Int,
    val sellerId: Int,
    val sellerNickname: String,
    val sellerReputationScore: Double?,
    val categoryName: String,
    val thumbnailUrl: String?
) {
    fun toDto(): AuctionListItemDto = AuctionListItemDto(
        auctionId = auctionId,
        name = name,
        startPrice = startPrice,
        currentHighestBid = currentHighestBid,
        buyNowPrice = buyNowPrice,
        status = status,
        endAt = endAt,
        bidCount = bidCount,
        seller = SellerDto(
            id = sellerId,
            nickname = sellerNickname,
            reputationScore = sellerReputationScore
        ),
        categoryName = categoryName,
        thumbnailUrl = thumbnailUrl
    )
}
