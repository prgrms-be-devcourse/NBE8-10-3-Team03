package com.back.domain.auction.auction.dto.response

import com.back.domain.auction.auction.entity.Auction
import com.back.domain.auction.auction.entity.AuctionStatus
import com.back.domain.auction.auction.entity.CancellerRole
import java.time.LocalDateTime

data class AuctionDetailResponse(
    val auctionId: Int?,
    val name: String?,
    val description: String?,
    val startPrice: Int?,
    val currentHighestBid: Int?,
    val buyNowPrice: Int?,
    val bidCount: Int?,
    val status: AuctionStatus?,
    val startAt: LocalDateTime?,
    val endAt: LocalDateTime?,
    val imageUrls: List<String>,
    val seller: SellerDto,
    val categoryName: String?,
    val winnerId: Int?,
    val closedAt: LocalDateTime?,
    val cancelledBy: Int?,
    val cancellerRole: CancellerRole?,
    val cancellerRoleDescription: String?
) {
    constructor(auction: Auction) : this(
        auctionId = auction.id,
        name = auction.name,
        description = auction.description,
        startPrice = auction.startPrice,
        currentHighestBid = auction.currentHighestBid,
        buyNowPrice = auction.buyNowPrice,
        bidCount = auction.bidCount,
        status = auction.status,
        startAt = auction.startAt,
        endAt = auction.endAt,
        imageUrls = auction.auctionImages.map { it.image.url },
        seller = SellerDto(
            auction.seller.id,
            auction.seller.nickname,
            auction.seller.reputation?.score
        ),
        categoryName = auction.category.name,
        winnerId = auction.winnerId,
        closedAt = auction.closedAt,
        cancelledBy = auction.cancelledBy,
        cancellerRole = auction.cancellerRole,
        cancellerRoleDescription = auction.cancellerRole?.description
    )
}
